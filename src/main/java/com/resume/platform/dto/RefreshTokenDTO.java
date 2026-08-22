package com.resume.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新AccessToken请求 DTO
 *
 * @author system
 */
@Data
public class RefreshTokenDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 刷新令牌（refreshToken）
     */
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
