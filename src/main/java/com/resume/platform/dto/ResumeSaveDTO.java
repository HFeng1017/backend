package com.resume.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 简历保存请求 DTO
 * 遵循阿里巴巴Java开发手册：
 * - DTO类必须实现Serializable
 * - 用户输入字段必须使用 @Size 限制长度，避免DB截断
 *
 * @author system
 */
@Data
public class ResumeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标用户ID（越权校验用）
     */
    @NotNull(message = "userId 不能为空")
    private Long userId;

    /**
     * 姓名
     */
    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    private String name;

    /**
     * 职位/求职意向
     */
    @Size(max = 100, message = "职位长度不能超过 100 个字符")
    private String title;

    /**
     * 头像图片URL
     */
    @Size(max = 255, message = "头像 URL 长度不能超过 255 个字符")
    private String avatar;

    /**
     * 电子邮箱
     */
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "电话长度不能超过 20 个字符")
    private String phone;

    /**
     * 个人简介
     */
    private String introduction;

    /**
     * 技能标签
     */
    private String skills;

    /**
     * 工作经历
     */
    private String experience;

    /**
     * 教育背景
     */
    private String education;

    /**
     * 项目经验
     */
    private String projects;
}
