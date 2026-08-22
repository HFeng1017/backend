package com.resume.platform.controller;

import com.resume.platform.common.Result;
import com.resume.platform.dto.*;
import com.resume.platform.entity.User;
import com.resume.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、Token刷新、重置密码等认证相关接口
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 登录结果Map初始容量：userId/username/role/accessToken/refreshToken 约5个
     */
    private static final int LOGIN_RESULT_CAPACITY = 8;

    /**
     * 图形验证码Map初始容量：code/image 2个
     */
    private static final int CAPTCHA_CAPACITY = 4;

    /**
     * 用户登录接口
     *
     * @param dto 登录请求体（username/password）
     * @return 登录结果，含tokens + 用户基本信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        log.info("收到登录请求: username={}", dto.getUsername());
        Map<String, String> tokens = userService.login(dto.getUsername(), dto.getPassword());
        User user = userService.getUserByUsername(dto.getUsername());
        Map<String, Object> result = new HashMap<>(LOGIN_RESULT_CAPACITY);
        result.putAll(tokens);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return Result.success(result);
    }

    /**
     * 刷新AccessToken
     *
     * @param dto 刷新请求体（refreshToken）
     * @return 新的accessToken
     */
    @PostMapping("/refresh")
    public Result<String> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        log.info("收到Token刷新请求");
        String accessToken = userService.refreshToken(dto.getRefreshToken());
        return Result.success(accessToken);
    }

    /**
     * 发送重置密码验证码
     *
     * @param dto 邮箱请求体
     * @return 发送成功标志
     */
    @PostMapping("/send-reset-code")
    public Result<Boolean> sendResetCode(@Valid @RequestBody SendResetCodeDTO dto) {
        log.info("收到重置密码验证码请求: email={}", dto.getEmail());
        boolean success = userService.sendResetCode(dto.getEmail());
        return Result.success(success);
    }

    /**
     * 校验验证码并重置密码
     *
     * @param dto 重置密码请求体
     * @return 重置成功标志
     */
    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        log.info("收到重置密码请求: email={}", dto.getEmail());
        boolean success = userService.resetPassword(
                dto.getEmail(),
                dto.getCode(),
                dto.getNewPassword()
        );
        return Result.success(success);
    }

    /**
     * 获取图形验证码（演示版：SVG格式，便于无额外依赖）
     *
     * @return 验证码明文 + 图片data-url
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        Map<String, String> result = new HashMap<>(CAPTCHA_CAPACITY);
        String code = String.valueOf((int) (Math.random() * 9000 + 1000));
        result.put("code", code);
        result.put("image", "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='40'%3E%3Crect width='120' height='40' fill='%23f0f0f0'/%3E%3Ctext x='60' y='28' font-size='24' text-anchor='middle' fill='%23333'%3E" + code + "%3C/text%3E%3C/svg%3E");
        log.debug("生成图形验证码: {}", code);
        return Result.success(result);
    }
}
