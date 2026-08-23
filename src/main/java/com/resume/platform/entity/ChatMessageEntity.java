package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话消息实体（持久化到MySQL）
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 * - 类字段添加Javadoc说明业务含义
 *
 * @author system
 */
@Data
@TableName("chat_message")
public class ChatMessageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID（关联chat_session表的id）
     */
    private Long sessionDbId;

    /**
     * 会话唯一标识（如 user:userId）
     */
    private String sessionId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 角色标识：system-系统提示，user-用户消息，assistant-AI回复
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息序号（同会话内按顺序递增）
     */
    private Integer seqNo;

    /**
     * 状态：1-正常，0-删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
