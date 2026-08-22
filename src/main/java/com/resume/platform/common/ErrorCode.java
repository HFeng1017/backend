package com.resume.platform.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * 遵循阿里巴巴Java开发手册：统一错误码规约
 *
 * @author system
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS(200, "success"),

    /**
     * 参数校验失败
     */
    PARAM_ERROR(400, "参数校验失败"),

    /**
     * 未登录或登录已过期
     */
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),

    /**
     * 无权限访问
     */
    FORBIDDEN(403, "无权限访问该资源"),

    /**
     * 业务异常通用码
     */
    BUSINESS_ERROR(500, "业务处理失败"),

    /**
     * 用户名或密码错误
     */
    LOGIN_FAILED(50001, "用户名或密码错误"),

    /**
     * 账号已禁用
     */
    ACCOUNT_DISABLED(50002, "账号已被禁用，请联系管理员"),

    /**
     * Token 无效或已过期
     */
    TOKEN_INVALID(50003, "登录凭证无效或已过期"),

    /**
     * 邮箱不存在
     */
    EMAIL_NOT_FOUND(50004, "邮箱地址不存在"),

    /**
     * 验证码错误
     */
    CODE_INVALID(50005, "验证码无效或已过期"),

    /**
     * 资源不存在
     */
    RESOURCE_NOT_FOUND(50006, "请求的资源不存在"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(50007, "文件上传失败"),

    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_FAILED(50008, "文件下载失败"),

    /**
     * 非法越权访问
     */
    PERMISSION_DENIED(50009, "无权访问该资源");

    private final Integer code;
    private final String message;
}
