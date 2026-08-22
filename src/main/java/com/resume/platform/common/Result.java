package com.resume.platform.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果封装
 * 遵循阿里巴巴Java开发手册：接口返回值必须封装Result&lt;T&gt;结构，禁止返回null
 *
 * @param <T> 业务数据泛型
 * @author system
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功返回（无数据）
     *
     * @param <T> 泛型
     * @return Result
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功返回（带数据）
     *
     * @param data 业务数据
     * @param <T>  泛型
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 失败返回（仅错误信息，使用通用业务错误码）
     *
     * @param message 错误提示
     * @param <T>     泛型
     * @return Result
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.BUSINESS_ERROR.getCode());
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（指定错误码+信息）
     *
     * @param code    错误码
     * @param message 错误提示
     * @param <T>     泛型
     * @return Result
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（通过ErrorCode枚举）
     *
     * @param errorCode 错误码
     * @param <T>       泛型
     * @return Result
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(errorCode.getMessage());
        return result;
    }
}
