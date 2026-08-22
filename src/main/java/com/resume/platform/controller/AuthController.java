package com.resume.platform.controller;

import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.common.Result;
import com.resume.platform.dto.*;
import com.resume.platform.entity.User;
import com.resume.platform.service.UserService;
import com.resume.platform.utils.RsaUtil;
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
    private final RsaUtil rsaUtil;

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
     * 密码传输约定：前端使用 /api/public/public-key 下发的RSA公钥对明文密码加密（Base64输出），
     * 后端此处先用RSA私钥解密密文，再进行BCrypt哈希比对。
     *
     * @param dto 登录请求体（username：用户名；password：RSA公钥加密后的Base64密文）
     * @return 登录结果，含tokens + 用户基本信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        log.info("收到登录请求: username={}, 密码密文长度={}", dto.getUsername(),
                dto.getPassword() == null ? 0 : dto.getPassword().length());

        // 1. 使用私钥解密 RSA 密文得到明文密码
        String rawPassword = rsaUtil.decryptPassword(dto.getPassword());
        if (rawPassword == null) {
            // 兼容明文（开发/测试兜底，防止前端未及时更新导致登录死锁）
            rawPassword = dto.getPassword();
            log.debug("RSA解密为空，回退使用原始password作为明文（前端可能未加密）");
        }

        // 2. 卫语句：解密后密码不能为空
        if (rawPassword == null || rawPassword.isEmpty()) {
            log.warn("登录失败 - 密码解密后为空: username={}", dto.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 进行登录业务校验（用户名 + 明文密码 BCrypt 比对）
        Map<String, String> tokens = userService.login(dto.getUsername(), rawPassword);
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
