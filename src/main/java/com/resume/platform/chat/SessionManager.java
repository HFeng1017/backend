package com.resume.platform.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AI对话会话管理器
 * 遵循阿里巴巴Java开发手册：
 * - 集合初始化指定初始容量
 * - 卫语句优先于嵌套if-else
 * - 日志使用占位符，避免字符串拼接
 *
 * 功能：
 * 1. 优先使用Redis缓存多轮对话历史
 * 2. Redis不可用时自动回退到内存存储（ConcurrentHashMap + 定时过期检查）
 * 3. 滑动窗口截断：超过最大消息条数时丢弃最早消息
 * 4. 流结束时将AI完整回复追加到会话历史
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private static final String SESSION_KEY_PREFIX = "chat:session:";

    private static final int SESSION_ID_LOG_LEN = 8;

    /**
     * Redis不可用时的内存存储回退
     */
    private final Map<String, SessionEntry> memoryFallback = new ConcurrentHashMap<>();

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Redis是否可用（启动时检查一次，后续失败则切换到内存模式）
     */
    private volatile boolean redisAvailable = true;

    @Value("${ai.session.max-history:20}")
    private int maxHistory;

    @Value("${ai.session.ttl-minutes:30}")
    private long ttlMinutes;

    @Value("${ai.session.system-prompt}")
    private String systemPrompt;

    /**
     * 内存存储条目（带过期时间戳）
     */
    private static class SessionEntry {
        final String json;
        final long expireAt;

        SessionEntry(String json, long expireAt) {
            this.json = json;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /**
     * 获取会话历史消息列表
     * 优先从Redis读取，Redis不可用时回退到内存存储
     */
    public List<ChatMessage> getMessages(String sessionId) {
        // 清理过期内存条目
        cleanupExpired();

        // 1. 尝试Redis
        if (redisAvailable) {
            try {
                String json = redisTemplate.opsForValue().get(buildKey(sessionId));
                if (json != null) {
                    List<ChatMessage> messages = objectMapper.readValue(json, new TypeReference<>() {});
                    if (!messages.isEmpty()) {
                        return messages;
                    }
                }
                // Redis中没有，继续往下走到新会话逻辑
            } catch (Exception e) {
                log.warn("Redis读取失败，切换到内存存储 sessionId={}*: {}", truncateSessionId(sessionId), e.getMessage());
                redisAvailable = false;
            }
        }

        // 2. 回退到内存存储
        SessionEntry entry = memoryFallback.get(sessionId);
        if (entry != null && !entry.isExpired()) {
            try {
                return objectMapper.readValue(entry.json, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("解析内存会话数据失败: {}", e.getMessage());
            }
        } else if (entry != null) {
            memoryFallback.remove(sessionId);
        }

        // 3. 新会话：首条消息为系统提示
        List<ChatMessage> newMessages = new ArrayList<>(maxHistory);
        newMessages.add(ChatMessage.ofSystem(systemPrompt));
        return newMessages;
    }

    /**
     * 追加用户消息和AI回复到会话历史
     * 执行滑动窗口截断：超出最大条数时丢弃最早的非系统消息
     */
    public void appendTurn(String sessionId, String userMessage, String assistantReply) {
        List<ChatMessage> messages = getMessages(sessionId);
        messages.add(ChatMessage.ofUser(userMessage));
        messages.add(ChatMessage.ofAssistant(assistantReply));

        // 滑动窗口截断
        if (messages.size() > maxHistory) {
            List<ChatMessage> truncated = new ArrayList<>(maxHistory + 1);
            truncated.add(messages.get(0)); // 系统提示始终保留
            int start = messages.size() - (maxHistory - 1);
            if (start < 1) {
                start = 1;
            }
            truncated.addAll(messages.subList(start, messages.size()));
            messages = truncated;
        }

        try {
            String json = objectMapper.writeValueAsString(messages);
            long expireAt = System.currentTimeMillis() + ttlMinutes * 60_000;

            // 尝试Redis
            if (redisAvailable) {
                try {
                    redisTemplate.opsForValue().set(buildKey(sessionId), json, ttlMinutes, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.warn("Redis写入失败，切换到内存存储 sessionId={}*: {}", truncateSessionId(sessionId), e.getMessage());
                    redisAvailable = false;
                    memoryFallback.put(sessionId, new SessionEntry(json, expireAt));
                }
            } else {
                // 直接用内存
                memoryFallback.put(sessionId, new SessionEntry(json, expireAt));
            }
            log.debug("会话历史已更新 sessionId={}*, 消息数={}", truncateSessionId(sessionId), messages.size());
        } catch (JsonProcessingException e) {
            log.error("序列化会话历史失败 sessionId={}*: {}", truncateSessionId(sessionId), e.getMessage());
        }
    }

    /**
     * 清除指定会话的历史记录
     */
    public void clearSession(String sessionId) {
        String key = buildKey(sessionId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis删除失败（忽略）: {}", e.getMessage());
        }
        memoryFallback.remove(sessionId);
        log.info("会话已清除 sessionId={}*", truncateSessionId(sessionId));
    }

    /**
     * 清理过期的内存存储条目
     */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        memoryFallback.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String buildKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String truncateSessionId(String sessionId) {
        if (sessionId == null) {
            return "null";
        }
        return sessionId.length() <= SESSION_ID_LOG_LEN
                ? sessionId
                : sessionId.substring(0, SESSION_ID_LOG_LEN);
    }
}
