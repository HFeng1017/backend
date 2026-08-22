package com.resume.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统权限配置更新 DTO
 * 遵循阿里巴巴Java开发手册：
 * - DTO类必须实现Serializable
 * - 枚举值限制使用正则校验（值只能是 "true" 或 "false"）
 * - 所有字段可选，只更新传入的字段
 *
 * @author system
 */
@Data
public class SystemConfigUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 布尔值正则（true/false）
     */
    private static final String BOOLEAN_REGEX = "^(true|false)$";

    /**
     * 游客是否可访问首页
     */
    @JsonProperty("guest_can_view_home")
    @Pattern(regexp = BOOLEAN_REGEX, message = "guest_can_view_home 只能是 true 或 false")
    private String guestCanViewHome;

    /**
     * 游客是否可查看简历
     */
    @JsonProperty("guest_can_view_resume")
    @Pattern(regexp = BOOLEAN_REGEX, message = "guest_can_view_resume 只能是 true 或 false")
    private String guestCanViewResume;

    /**
     * 游客是否可查看项目
     */
    @JsonProperty("guest_can_view_projects")
    @Pattern(regexp = BOOLEAN_REGEX, message = "guest_can_view_projects 只能是 true 或 false")
    private String guestCanViewProjects;

    /**
     * 普通用户是否可编辑简历
     */
    @JsonProperty("user_can_edit_resume")
    @Pattern(regexp = BOOLEAN_REGEX, message = "user_can_edit_resume 只能是 true 或 false")
    private String userCanEditResume;

    /**
     * 普通用户是否可查看项目
     */
    @JsonProperty("user_can_view_projects")
    @Pattern(regexp = BOOLEAN_REGEX, message = "user_can_view_projects 只能是 true 或 false")
    private String userCanViewProjects;

    /**
     * 是否允许新用户注册
     */
    @JsonProperty("site_registration_enabled")
    @Pattern(regexp = BOOLEAN_REGEX, message = "site_registration_enabled 只能是 true 或 false")
    private String siteRegistrationEnabled;
}
