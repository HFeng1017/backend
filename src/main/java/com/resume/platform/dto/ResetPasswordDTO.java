package com.resume.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码请求 DTO
 *
 * @author system
 */
@Data
public class ResetPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户注册邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 邮箱收到的6位验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;

    /**
     * 新密码（明文）
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 个字符之间")
    private String newPassword;
}
