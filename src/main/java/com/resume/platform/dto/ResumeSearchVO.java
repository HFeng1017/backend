package com.resume.platform.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 简历搜索结果VO
 *
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 *
 * @author system
 */
@Data
public class ResumeSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 简历ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 职位/求职意向
     */
    private String title;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 技能标签（逗号分隔）
     */
    private String skills;

    /**
     * 个人简介（用于结果摘要）
     */
    private String introduction;

    /**
     * 相关性得分（姓名精准=100最高，按 姓名全文40 / 职位30 / 技能20 / 简介10 加权）
     */
    private Double relevance;

    /**
     * 命中字段：name-姓名精准 / title-职位 / skills-技能 / introduction-简介 / fallback-模糊兜底
     */
    private String matchedField;
}
