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
     * 登录密码
     * 双轨兼容：
     *   - HTTPS/WebCrypto 可用：RSA-OAEP 加密后 Base64 密文（RSA-2048 ≈ 344字符）
     *   - HTTP/WebCrypto 不可用：回退明文传输（6-20字符）
     * 因此放宽 max 到 2048 兼容未来 RSA-4096 密文，min 放 1 兼容所有场景
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 2048, message = "密码长度必须在 1-2048 个字符之间")
    private String password;
}
