-- AI对话历史表结构
-- 创建时间：2026-08-24
-- 数据库：MySQL 8.0+

-- 会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识（如 user:userId）',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_db_id` BIGINT NOT NULL COMMENT '会话数据库ID（关联chat_session.id）',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：system-系统提示，user-用户消息，assistant-AI回复',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `seq_no` INT NOT NULL DEFAULT 0 COMMENT '消息序号（同会话内递增）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_session_db_id` (`session_db_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_seq_no` (`session_id`, `seq_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';
