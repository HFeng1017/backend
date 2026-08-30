package com.resume.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 测试代理请求 DTO
 * 前端测试工具提交的接口测试请求，后端代为转发以绕过浏览器 CORS。
 *
 * @author system
 */
@Data
public class ProxyRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP方法：GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS
     */
    @NotBlank(message = "请求方法不能为空")
    private String method;

    /**
     * 目标URL（完整地址）
     */
    @NotBlank(message = "请求URL不能为空")
    private String url;

    /**
     * 请求头（key-value）
     */
    private Map<String, String> headers;

    /**
     * 查询参数（key-value）
     */
    private Map<String, String> params;

    /**
     * 请求体（raw文本，如JSON/XML）
     */
    private String body;

    /**
     * 请求体类型：json/form-urlencoded/raw
     */
    private String bodyType;

    /**
     * 是否跟随重定向
     */
    private Boolean followRedirects = Boolean.TRUE;

    /**
     * 超时秒数
     */
    private Integer timeoutSeconds = 30;
}
