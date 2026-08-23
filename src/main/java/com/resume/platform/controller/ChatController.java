package com.resume.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.platform.chat.ChatMessage;
import com.resume.platform.chat.SessionManager;
import com.resume.platform.common.Result;
import com.resume.platform.dto.ChatRequestDTO;
import com.resume.platform.entity.ChatSession;
import com.resume.platform.service.ChatHistoryService;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AI对话控制器
 *
 * 使用 StreamingResponseBody 实现 SSE 流式响应。
 * 支持对话历史持久化到 MySQL，提供历史查询接口。
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
@RequiredArgsConstructor
public class ChatController {

    private final SessionManager sessionManager;
    private final ChatHistoryService chatHistoryService;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SSE_DONE = "[DONE]";

    @org.springframework.beans.factory.annotation.Value("${ai.zhipu.model:glm-4}")
    private String model;

    @org.springframework.beans.factory.annotation.Value("${ai.zhipu.temperature:0.7}")
    private double temperature;

    @org.springframework.beans.factory.annotation.Value("${ai.zhipu.max-tokens:2048}")
    private int maxTokens;

    /**
     * SSE流式对话接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamChat(@Valid @RequestBody ChatRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = extractUserId(auth);
        String sessionId = resolveSessionId(dto, auth);
        String safeId = truncateId(sessionId);
        log.info("AI对话请求 sessionId={}*, length={}, userId={}", safeId, dto.getMessage().length(), userId);

        List<ChatMessage> messages = new ArrayList<>(sessionManager.getMessages(sessionId));
        messages.add(ChatMessage.ofUser(dto.getMessage()));

        List<Map<String, String>> messageMaps = messages.stream()
                .map(m -> {
                    Map<String, String> map = new HashMap<>(2);
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    return map;
                }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> body = new HashMap<>(8);
        body.put("model", model);
        body.put("messages", messageMaps);
        body.put("stream", true);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        return outputStream -> {
            StringBuilder replyBuilder = new StringBuilder(512);
            StringBuilder sseBuffer = new StringBuilder(1024);

            try {
                writeEvent(outputStream, "ready", "连接已建立");
                flush(outputStream);

                aiWebClient.post()
                        .uri("/api/paas/v4/chat/completions")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                        .timeout(Duration.ofSeconds(120))
                        .toIterable()
                        .forEach(buffer -> {
                            String chunk = buffer.toString(StandardCharsets.UTF_8);
                            sseBuffer.append(chunk);
                            processSseBuffer(sseBuffer, replyBuilder, outputStream);
                        });

                // 处理缓冲区末尾可能残留的不完整事件
                if (!sseBuffer.isEmpty()) {
                    String remaining = sseBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        parseSseEventBlock(remaining, replyBuilder, outputStream);
                    }
                }

                String reply = replyBuilder.toString();
                if (!reply.isEmpty()) {
                    sessionManager.appendTurn(sessionId, dto.getMessage(), reply);
                    // 持久化到MySQL
                    try {
                        chatHistoryService.saveTurn(sessionId, userId, dto.getMessage(), reply);
                    } catch (Exception e) {
                        log.warn("对话历史持久化MySQL失败 sessionId={}*: {}", safeId, e.getMessage());
                    }
                }

                writeEvent(outputStream, "done", SSE_DONE);
                flush(outputStream);

                // 延迟确保chunked终止标记发送
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                log.info("AI对话完成 sessionId={}*, 回复长度={}", safeId, reply.length());

            } catch (Exception e) {
                log.error("AI对话异常 sessionId={}*: {}", safeId, e.getMessage(), e);
                try {
                    writeEvent(outputStream, "error", "服务异常: " + e.getMessage());
                    flush(outputStream);
                } catch (java.io.IOException ignored) {
                }
            }
        };
    }

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions() {
        Long userId = extractUserId(SecurityContextHolder.getContext().getAuthentication());
        List<ChatSession> sessions = chatHistoryService.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 获取会话详情（包含消息列表）
     */
    @GetMapping("/session/{sessionId}")
    public Result<ChatHistoryService.SessionDetailDTO> getSessionDetail(@PathVariable String sessionId) {
        Long userId = extractUserId(SecurityContextHolder.getContext().getAuthentication());
        ChatHistoryService.SessionDetailDTO detail = chatHistoryService.getSessionDetail(sessionId);
        if (detail == null) {
            return Result.error("会话不存在");
        }
        // 权限校验
        if (!detail.session().getUserId().equals(userId)) {
            return Result.error("无权访问此会话");
        }
        return Result.success(detail);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Boolean> deleteSession(@PathVariable String sessionId) {
        Long userId = extractUserId(SecurityContextHolder.getContext().getAuthentication());
        chatHistoryService.deleteSession(sessionId, userId);
        // 同时清除Redis缓存
        sessionManager.clearSession(sessionId);
        return Result.success(true);
    }

    /**
     * 清空所有会话
     */
    @DeleteMapping("/sessions")
    public Result<Boolean> clearAllSessions() {
        Long userId = extractUserId(SecurityContextHolder.getContext().getAuthentication());
        chatHistoryService.clearAllSessions(userId);
        return Result.success(true);
    }

    /**
     * 清空当前会话（Redis + MySQL）
     */
    @PostMapping("/clear")
    public Result<Boolean> clearSession(@RequestBody ChatRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = extractUserId(auth);
        String sessionId = resolveSessionId(dto, auth);
        sessionManager.clearSession(sessionId);
        chatHistoryService.deleteSession(sessionId, userId);
        return Result.success(true);
    }

    /**
     * 处理 SSE 缓冲区，按 \n\n 分割完整事件
     */
    private void processSseBuffer(StringBuilder buffer, StringBuilder replyBuilder, OutputStream os) {
        int idx;
        while ((idx = buffer.indexOf("\n\n")) != -1) {
            String eventBlock = buffer.substring(0, idx);
            buffer.delete(0, idx + 2);
            parseSseEventBlock(eventBlock, replyBuilder, os);
        }
    }

    /**
     * 解析一个完整的 SSE 事件块
     */
    private void parseSseEventBlock(String eventBlock, StringBuilder replyBuilder, OutputStream os) {
        if (eventBlock == null || eventBlock.isBlank()) {
            return;
        }

        String[] lines = eventBlock.split("\n");
        StringBuilder jsonData = new StringBuilder(256);
        boolean isDataLine = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith(":")) {
                continue;
            }

            if (line.startsWith("data:")) {
                String value = line.substring(5).trim();
                if (value.isEmpty()) {
                    isDataLine = true;
                } else {
                    if (SSE_DONE.equals(value)) {
                        return;
                    }
                    jsonData.setLength(0);
                    jsonData.append(value);
                    isDataLine = false;
                }
            } else if (isDataLine) {
                jsonData.append(line);
                isDataLine = false;
            }
        }

        if (jsonData.length() > 0) {
            extractAndWriteToken(jsonData.toString(), replyBuilder, os);
        }
    }

    /**
     * 从 JSON 响应中提取 Token 并推送给客户端
     */
    private void extractAndWriteToken(String json, StringBuilder replyBuilder, OutputStream os) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
            if (!contentNode.isMissingNode() && !contentNode.asText().isEmpty()) {
                String token = contentNode.asText();
                replyBuilder.append(token);
                writeEvent(os, "token", token);
                flush(os);
            }
        } catch (Exception e) {
            log.debug("解析AI响应失败: {}, 原文: {}", e.getMessage(),
                    json.length() > 100 ? json.substring(0, 100) + "..." : json);
        }
    }

    /**
     * 从认证信息中提取用户ID
     * userId存储在authentication.details中（由JwtAuthenticationFilter设置）
     */
    private Long extractUserId(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof Long) {
            return (Long) auth.getDetails();
        }
        if (auth != null && auth.getDetails() instanceof Number) {
            return ((Number) auth.getDetails()).longValue();
        }
        log.debug("提取userId失败, details={}", auth != null ? auth.getDetails() : "null");
        return 0L;
    }

    /**
     * 解析会话ID
     */
    private String resolveSessionId(ChatRequestDTO dto, Authentication auth) {
        if (dto.getSessionId() != null && !dto.getSessionId().isBlank()) {
            return dto.getSessionId();
        }
        Long userId = extractUserId(auth);
        return "user:" + userId;
    }

    /**
     * 截断ID用于日志
     */
    private String truncateId(String id) {
        if (id == null) {
            return "null";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private void writeEvent(OutputStream os, String event, String data) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("event:").append(event).append("\n");
        sb.append("data:").append(data).append("\n\n");
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void flush(OutputStream os) throws java.io.IOException {
        os.flush();
    }
}
