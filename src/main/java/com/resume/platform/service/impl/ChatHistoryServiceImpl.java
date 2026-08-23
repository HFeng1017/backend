package com.resume.platform.service.impl;

import com.resume.platform.chat.ChatMessage;
import com.resume.platform.entity.ChatMessageEntity;
import com.resume.platform.entity.ChatSession;
import com.resume.platform.mapper.ChatMessageEntityMapper;
import com.resume.platform.mapper.ChatSessionMapper;
import com.resume.platform.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI对话历史服务实现
 * 遵循阿里巴巴Java开发手册：
 * - 使用构造器注入
 * - 集合初始化指定初始容量
 * - 卫语句优先于嵌套if-else
 * - 日志使用占位符
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageEntityMapper chatMessageMapper;

    /**
     * 保存一轮对话（用户消息 + AI回复）
     * 使用 @Transactional 保证事务一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTurn(String sessionId, Long userId, String userMessage, String aiReply) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 查找或创建会话
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            session = new ChatSession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setTitle(generateTitle(userMessage));
            session.setMessageCount(0);
            session.setStatus(1);
            session.setCreateTime(now);
            session.setUpdateTime(now);
            session.setLastMessageTime(now);
            chatSessionMapper.insert(session);
            log.info("创建新会话 sessionId={}*, userId={}", truncateSessionId(sessionId), userId);
        }

        // 2. 获取当前消息序号
        int currentCount = chatMessageMapper.countBySessionId(sessionId);

        // 3. 保存用户消息
        ChatMessageEntity userMsg = createMessage(session.getId(), sessionId, userId, "user",
                userMessage, currentCount + 1, now);
        chatMessageMapper.insert(userMsg);

        // 4. 保存AI回复
        ChatMessageEntity aiMsg = createMessage(session.getId(), sessionId, userId, "assistant",
                aiReply, currentCount + 2, now);
        chatMessageMapper.insert(aiMsg);

        // 5. 更新会话统计信息
        session.setMessageCount(currentCount + 2);
        session.setLastMessageTime(now);
        session.setUpdateTime(now);
        chatSessionMapper.updateById(session);

        log.debug("保存对话记录 sessionId={}*, 本轮消息数=2, 累计={}", truncateSessionId(sessionId), session.getMessageCount());
    }

    /**
     * 查询会话的历史消息列表
     */
    @Override
    public List<ChatMessage> getSessionMessages(String sessionId) {
        List<ChatMessageEntity> entities = chatMessageMapper.selectBySessionId(sessionId);
        if (entities.isEmpty()) {
            return new ArrayList<>(0);
        }
        return entities.stream()
                .map(this::convertToChatMessage)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的会话列表
     */
    @Override
    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionMapper.selectListByUserId(userId);
    }

    /**
     * 查询会话详情
     */
    @Override
    public SessionDetailDTO getSessionDetail(String sessionId) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            return null;
        }
        List<ChatMessage> messages = getSessionMessages(sessionId);
        return new SessionDetailDTO(session, messages);
    }

    /**
     * 删除会话（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            log.warn("删除会话失败：会话不存在或无权访问 sessionId={}*, userId={}", truncateSessionId(sessionId), userId);
            return;
        }
        session.setStatus(0);
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        // 逻辑删除消息
        ChatMessageEntity updateMsg = new ChatMessageEntity();
        updateMsg.setStatus(0);
        updateMsg.setSessionId(sessionId);
        chatMessageMapper.update(updateMsg,
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessageEntity>()
                        .eq("session_id", sessionId));

        log.info("会话已删除 sessionId={}*, userId={}", truncateSessionId(sessionId), userId);
    }

    /**
     * 清空用户的所有会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllSessions(Long userId) {
        // 逻辑删除所有会话
        ChatSession sessionUpdate = new ChatSession();
        sessionUpdate.setStatus(0);
        chatSessionMapper.update(sessionUpdate,
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .eq("status", 1));

        log.info("清空所有会话 userId={}", userId);
    }

    /**
     * 创建消息实体
     */
    private ChatMessageEntity createMessage(Long sessionDbId, String sessionId, Long userId,
                                            String role, String content, int seqNo, LocalDateTime now) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionDbId(sessionDbId);
        entity.setSessionId(sessionId);
        entity.setUserId(userId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setSeqNo(seqNo);
        entity.setStatus(1);
        entity.setCreateTime(now);
        return entity;
    }

    /**
     * 实体转DTO
     */
    private ChatMessage convertToChatMessage(ChatMessageEntity entity) {
        return new ChatMessage(entity.getRole(), entity.getContent(),
                entity.getCreateTime() != null ? entity.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null);
    }

    /**
     * 生成会话标题（取用户消息前30字）
     */
    private String generateTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "新对话";
        }
        String trimmed = userMessage.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) + "..." : trimmed;
    }

    /**
     * 截断会话ID用于日志
     */
    private String truncateSessionId(String sessionId) {
        if (sessionId == null) {
            return "null";
        }
        return sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8);
    }
}
