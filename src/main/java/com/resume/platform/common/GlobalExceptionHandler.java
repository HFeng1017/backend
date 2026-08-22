package com.resume.platform.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 遵循阿里巴巴Java开发手册：区分业务异常与系统异常；禁止把底层异常堆栈返回给前端；ERROR级日志必须输出堆栈
 *
 * @author system
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     * 业务异常只记录WARN日志（不打印全量堆栈，节省磁盘），返回友好错误提示
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @RequestBody @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败(@Valid): {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理 @RequestParam / @PathVariable 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败(Constraint): {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理其他未捕获异常 —— 属于系统异常
     * 必须：1) 打印完整ERROR堆栈便于排查  2) 返回通用错误文案，不暴露敏感底层信息
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统未捕获异常: ", e);
        return Result.error(ErrorCode.BUSINESS_ERROR.getCode(), "服务器繁忙，请稍后再试");
    }
}
