package com.resume.platform.service;

import com.resume.platform.chat.ChatMessage;
import com.resume.platform.entity.ChatSession;

import java.util.List;

/**
 * AI对话历史服务接口
 * 遵循阿里巴巴Java开发手册：
 * - Service接口定义业务方法，不涉及实现细节
 *
 * @author system
 */
public interface ChatHistoryService {

    /**
     * 保存一轮对话（用户消息 + AI回复）
     *
     * @param sessionId    会话唯一标识
     * @param userId       用户ID
     * @param userMessage  用户消息
     * @param aiReply      AI回复
     */
    void saveTurn(String sessionId, Long userId, String userMessage, String aiReply);

    /**
     * 查询会话的历史消息列表
     *
     * @param sessionId 会话唯一标识
     * @return 消息列表
     */
    List<ChatMessage> getSessionMessages(String sessionId);

    /**
     * 查询用户的会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> getUserSessions(Long userId);

    /**
     * 查询会话详情（包含消息列表）
     *
     * @param sessionId 会话唯一标识
     * @return 会话详情DTO
     */
    SessionDetailDTO getSessionDetail(String sessionId);

    /**
     * 删除会话（逻辑删除）
     *
     * @param sessionId 会话唯一标识
     * @param userId    用户ID
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * 清空用户的所有会话
     *
     * @param userId 用户ID
     */
    void clearAllSessions(Long userId);

    /**
     * 会话详情DTO
     */
    record SessionDetailDTO(ChatSession session, List<ChatMessage> messages) {
    }
}
