package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话会话实体
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 * - 类字段添加Javadoc说明业务含义
 *
 * @author system
 */
@Data
@TableName("chat_session")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一标识（如 user:userId）
     */
    private String sessionId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 会话标题（取首条用户消息前30字作为标题）
     */
    private String title;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 状态：1-正常，0-删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
