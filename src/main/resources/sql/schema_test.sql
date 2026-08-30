-- ============================================================
-- 测试工具模块表结构（单体后端）
-- 用途：网页测试工具的用例持久化 + 按用户开关权限
-- 注意：CREATE TABLE IF NOT EXISTS 不会给已存在的表加列，
--       本文件为独立迁移脚本，首次启用测试模块时执行一次。
-- ============================================================

USE resume_platform;

-- 用户权限开关表（按用户维度覆盖功能开关）
CREATE TABLE IF NOT EXISTS user_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    permission_key VARCHAR(50) NOT NULL COMMENT '权限标识：test_tool/project_view/resume_edit/chat_use',
    enabled TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_perm (user_id, permission_key),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户级权限开关';

-- 测试用例表（按用户保存接口/网页测试用例）
CREATE TABLE IF NOT EXISTS test_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(100) NOT NULL COMMENT '用例名称',
    type VARCHAR(20) DEFAULT 'api' COMMENT 'api-接口测试 page-网页测试',
    method VARCHAR(10) DEFAULT 'GET' COMMENT 'HTTP方法',
    url VARCHAR(500) COMMENT '请求URL',
    headers TEXT COMMENT '请求头JSON',
    params TEXT COMMENT '查询参数JSON',
    body TEXT COMMENT '请求体',
    expected_status INT COMMENT '预期状态码',
    expected_body TEXT COMMENT '预期响应包含文本（断言）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例';

-- 测试执行记录表（用于生成MD报告）
CREATE TABLE IF NOT EXISTS test_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '执行用户ID',
    case_id BIGINT COMMENT '关联用例ID',
    case_name VARCHAR(100) COMMENT '用例名称',
    method VARCHAR(10),
    url VARCHAR(500),
    status INT COMMENT '实际HTTP状态码',
    elapsed_ms BIGINT COMMENT '耗时毫秒',
    passed TINYINT DEFAULT 0 COMMENT '1-通过 0-失败',
    error_msg TEXT COMMENT '失败原因',
    response_preview TEXT COMMENT '响应预览（截断）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试执行记录';
