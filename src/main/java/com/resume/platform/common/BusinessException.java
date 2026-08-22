package com.resume.platform.common;

import lombok.Getter;

/**
 * 业务异常类
 * 用于区分业务逻辑异常与系统异常，避免将底层错误信息直接暴露给前端
 *
 * @author system
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String extraMessage) {
        super(errorCode.getMessage() + ": " + extraMessage);
        this.code = errorCode.getCode();
    }
}
