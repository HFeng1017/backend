CREATE DATABASE IF NOT EXISTS resume_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE resume_platform;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'user',
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50),
    title VARCHAR(100),
    avatar VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    introduction TEXT,
    skills TEXT,
    experience TEXT,
    education TEXT,
    projects TEXT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    FULLTEXT INDEX ft_resume_name (name) WITH PARSER ngram,
    FULLTEXT INDEX ft_resume_title (title) WITH PARSER ngram,
    FULLTEXT INDEX ft_resume_skills (skills) WITH PARSER ngram,
    FULLTEXT INDEX ft_resume_intro (introduction) WITH PARSER ngram
);

-- 已有表的平滑升级（单列全文索引，支持按字段加权打分；重复执行报索引已存在可忽略）
-- ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_name (name) WITH PARSER ngram;
-- ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_title (title) WITH PARSER ngram;
-- ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_skills (skills) WITH PARSER ngram;
-- ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_intro (introduction) WITH PARSER ngram;

CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100),
    description TEXT,
    file_url VARCHAR(255),
    file_type VARCHAR(50),
    file_size BIGINT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(255),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入默认权限配置（幂等：已存在则更新，不存在则插入）
INSERT INTO system_config (config_key, config_value, description) VALUES
('guest_can_view_home', 'true', '游客是否可访问首页'),
('guest_can_view_resume', 'true', '游客是否可查看简历'),
('guest_can_view_projects', 'false', '游客是否可查看项目'),
('user_can_edit_resume', 'true', '普通用户是否可编辑简历'),
('user_can_view_projects', 'true', '普通用户是否可查看项目'),
('site_registration_enabled', 'true', '是否允许新用户注册')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description);

-- 插入测试用户 (密码都是: admin123 / user123)（幂等）
INSERT INTO user (username, password, email, phone, role) VALUES 
('admin', 'admin123_bcrypt_hash_placeholder', 'admin@example.com', '13800138000', 'admin'),
('user', 'user123_bcrypt_hash_placeholder', 'user@example.com', '13800138001', 'user')
ON DUPLICATE KEY UPDATE password = VALUES(password), email = VALUES(email), phone = VALUES(phone), role = VALUES(role);
