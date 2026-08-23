package com.resume.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.platform.chat.ChatMessage;
import com.resume.platform.chat.SessionManager;
import com.resume.platform.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI服务实现类
 * 核心职责：
 * 1. 使用WebClient（非阻塞）调用智谱AI v4流式接口
 * 2. 解析SSE响应，逐Token推送到SseEmitter
 * 3. 流结束时将完整回复存入Redis
 * 4. 完整的异常处理，确保SseEmitter总是正确关闭
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final WebClient aiWebClient;
    private final SessionManager sessionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.zhipu.model:glm-4}")
    private String model;

    @Value("${ai.zhipu.temperature:0.7}")
    private double temperature;

    @Value("${ai.zhipu.max-tokens:2048}")
    private int maxTokens;

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE = "[DONE]";
    private static final int REQUEST_BODY_CAPACITY = 8;

    @Override
    public void streamChat(List<ChatMessage> messages, String sessionId, String userMessage, SseEmitter emitter) {
        // 1. 构建请求体
        List<Map<String, String>> messageMaps = messages.stream()
                .map(m -> {
                    Map<String, String> map = new HashMap<>(2);
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>(REQUEST_BODY_CAPACITY);
        requestBody.put("model", model);
        requestBody.put("messages", messageMaps);
        requestBody.put("stream", true);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        // 2. 累积AI回复
        StringBuilder fullReply = new StringBuilder(512);

        // 3. 订阅智谱AI流式响应
        Disposable subscription = aiWebClient.post()
                .uri("/api/paas/v4/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        line -> handleSseLine(line, fullReply, emitter),
                        error -> handleStreamError(error, sessionId, userMessage, emitter),
                        () -> handleStreamComplete(fullReply, sessionId, userMessage, emitter)
                );

        // 4. 注册SseEmitter回调
        emitter.onCompletion(() -> {
            log.debug("SSE连接完成 sessionId={}*", truncate(sessionId));
            subscription.dispose();
        });
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时 sessionId={}*", truncate(sessionId));
            subscription.dispose();
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        emitter.onError(t -> {
            log.warn("SSE连接异常 sessionId={}*: {}", truncate(sessionId), t.getMessage());
            subscription.dispose();
        });
    }

    /**
     * 处理SSE单行数据
     */
    private void handleSseLine(String line, StringBuilder fullReply, SseEmitter emitter) {
        if (line == null || line.isEmpty()) {
            return;
        }

        // 智谱AI的SSE格式：data: {json}\n\n
        // 可能一行包含多个data事件，需要按\n分割
        String[] lines = line.split("\n");
        for (String singleLine : lines) {
            if (!singleLine.startsWith(SSE_DATA_PREFIX)) {
                continue;
            }
            String data = singleLine.substring(SSE_DATA_PREFIX.length()).trim();
            if (SSE_DONE.equals(data)) {
                return;
            }
            try {
                JsonNode root = objectMapper.readTree(data);
                JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
                if (!contentNode.isMissingNode() && !contentNode.asText().isEmpty()) {
                    String token = contentNode.asText();
                    fullReply.append(token);
                    // 推送单个Token，使用SseEmitter.event()确保正确的SSE格式
                    emitter.send(SseEmitter.event().name("token").data(token));
                }
            } catch (Exception e) {
                log.debug("解析智谱AI SSE行失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 处理流异常
     * 使用complete()而非completeWithError()，避免Servlet容器发送非200状态码
     * 导致浏览器报告ERR_INCOMPLETE_CHUNKED_ENCODING
     */
    private void handleStreamError(Throwable error, String sessionId, String userMessage, SseEmitter emitter) {
        log.error("调用智谱AI失败 sessionId={}*: {}", truncate(sessionId), error.getMessage());

        // 先通知前端错误事件
        try {
            emitter.send(SseEmitter.event().name("error").data("AI服务暂时不可用，请稍后重试"));
        } catch (IOException e) {
            log.warn("推送错误事件失败: {}", e.getMessage());
        }

        // 确保emitter正常关闭（200 OK），而非completeWithError导致非200状态码
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理流正常结束
     */
    private void handleStreamComplete(StringBuilder fullReply, String sessionId,
                                      String userMessage, SseEmitter emitter) {
        String reply = fullReply.toString();

        // 持久化会话历史
        try {
            sessionManager.appendTurn(sessionId, userMessage, reply);
        } catch (Exception e) {
            log.warn("持久化会话历史失败 sessionId={}*: {}", truncate(sessionId), e.getMessage());
        }

        // 通知前端完成
        try {
            emitter.send(SseEmitter.event().name("done").data(SSE_DONE));
        } catch (IOException e) {
            log.debug("推送完成事件失败（客户端可能已断开）");
        }

        // 确保emitter关闭
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }

        log.info("AI对话完成 sessionId={}*, 回复长度={}", truncate(sessionId), reply.length());
    }

    private String truncate(String str) {
        if (str == null) {
            return "null";
        }
        return str.length() <= 8 ? str : str.substring(0, 8);
    }
}
