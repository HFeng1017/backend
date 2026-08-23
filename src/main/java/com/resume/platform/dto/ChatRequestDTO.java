package com.resume.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * AI聊天请求DTO
 * 遵循阿里巴巴Java开发手册：
 * - 入参必须使用@Valid校验
 * - 字段长度限制防止恶意超长输入
 * - 禁止使用魔法值，长度阈值定义为显式数字
 *
 * @author system
 */
@Data
public class ChatRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户消息内容
     * 长度限制：1-2000字符（覆盖正常对话场景，防止超长Prompt攻击）
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字符")
    private String message;

    /**
     * 会话ID（可选）
     * 前端传入已有会话ID可继续多轮对话，为空则使用JWT中的用户ID
     */
    @Size(max = 64, message = "会话ID格式不正确")
    private String sessionId;
}
