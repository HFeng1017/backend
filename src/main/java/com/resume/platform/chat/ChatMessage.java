package com.resume.platform.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI对话消息实体
 * 遵循阿里巴巴Java开发手册：
 * - 实体类必须实现Serializable接口
 * - 使用Lombok注解简化样板代码
 *
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色标识：system（系统提示）/ user（用户）/ assistant（AI回复）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳（毫秒），用于排序和审计
     */
    private Long timestamp;

    /**
     * 工厂方法：创建用户消息
     *
     * @param content 用户输入内容
     * @return ChatMessage
     */
    public static ChatMessage ofUser(String content) {
        return new ChatMessage("user", content, System.currentTimeMillis());
    }

    /**
     * 工厂方法：创建AI回复消息
     *
     * @param content AI回复内容
     * @return ChatMessage
     */
    public static ChatMessage ofAssistant(String content) {
        return new ChatMessage("assistant", content, System.currentTimeMillis());
    }

    /**
     * 工厂方法：创建系统提示消息
     *
     * @param content 系统提示词
     * @return ChatMessage
     */
    public static ChatMessage ofSystem(String content) {
        return new ChatMessage("system", content, System.currentTimeMillis());
    }
}
