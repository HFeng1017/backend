package com.resume.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求 DTO
 * 遵循阿里巴巴Java开发手册：
 * - DTO类必须实现Serializable
 * - 所有接收参数必须使用Bean Validation做前置校验
 *
 * @author system
 */
@Data
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 个字符之间")
    private String username;

    /**
     * 登录密码（前端使用RSA公钥加密后的Base64密文；RSA-2048密文长度为344字符）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 1024, message = "密码长度必须在 6-1024 个字符之间")
    private String password;
}
