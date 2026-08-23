package com.resume.platform.service;

import com.resume.platform.chat.ChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI服务接口
 * 定义调用大模型的核心行为
 *
 * @author system
 */
public interface AiService {

    /**
     * 流式调用智谱AI，将Token逐个推送到SseEmitter
     * 流结束后将完整AI回复通过SessionManager存入Redis
     *
     * @param messages   完整对话历史（含系统提示）
     * @param sessionId  会话ID（用于流结束后持久化）
     * @param userMessage 用户当前消息原文（用于持久化）
     * @param emitter    SSE推送器
     */
    void streamChat(List<ChatMessage> messages, String sessionId, String userMessage, SseEmitter emitter);
}
