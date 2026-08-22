package com.resume.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送重置密码验证码请求 DTO
 *
 * @author system
 */
@Data
public class SendResetCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户注册邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
