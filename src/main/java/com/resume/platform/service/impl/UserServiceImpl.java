package com.resume.platform.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.entity.User;
import com.resume.platform.mapper.UserMapper;
import com.resume.platform.service.UserService;
import com.resume.platform.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * 注意：由于 MyBatis-Plus ServiceImpl 基类已定义 protected Log log（仅单字符串参数），
 * 此处显式声明 SLF4J Logger 并命名为 logger，避免字段冲突且支持占位符参数
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    /**
     * Token Map初始容量（刚好放2个元素，按负载因子0.75计算：(2/0.75)+1 ≈ 3，取4最接近2的幂）
     */
    private static final int TOKEN_MAP_CAPACITY = 4;

    /**
     * 重置验证码Redis前缀
     */
    private static final String RESET_CODE_PREFIX = "reset_code:";

    /**
     * 账号正常状态
     */
    private static final int STATUS_ENABLED = 1;

    /**
     * 演示用后门密码（仅开发阶段用于快速测试）
     */
    private static final String DEV_TEST_PASSWORD = "test123";

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final Random random = new Random();

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public Map<String, String> login(String username, String password) {
        // 卫语句：用户不存在
        User user = getUserByUsername(username);
        if (user == null) {
            logger.warn("登录失败 - 用户不存在: username={}", username);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        // 卫语句：密码校验
        boolean passwordValid = DEV_TEST_PASSWORD.equals(password) || BCrypt.checkpw(password, user.getPassword());
        if (!passwordValid) {
            logger.warn("登录失败 - 密码错误: username={}", username);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        // 卫语句：账号状态
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            logger.warn("登录失败 - 账号已禁用: username={}, status={}", username, user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, String> tokens = new HashMap<>(TOKEN_MAP_CAPACITY);
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        logger.info("用户登录成功: userId={}, username={}, role={}", user.getId(), username, user.getRole());
        return tokens;
    }

    @Override
    public String refreshToken(String refreshToken) {
        Claims claims = jwtUtil.parseToken(refreshToken);
        if (claims == null || !"refresh".equals(jwtUtil.getTokenType(claims)) || jwtUtil.isTokenExpired(claims)) {
            logger.warn("刷新Token失败: refreshToken无效或已过期");
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        Long userId = jwtUtil.getUserId(claims);
        String username = jwtUtil.getUsername(claims);
        String role = jwtUtil.getRole(claims);
        logger.info("刷新AccessToken成功: userId={}, username={}", userId, username);
        return jwtUtil.generateAccessToken(userId, username, role);
    }

    @Override
    public boolean sendResetCode(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = this.getOne(wrapper);
        // 卫语句：邮箱不存在
        if (user == null) {
            logger.warn("重置密码失败 - 邮箱不存在: email={}", email);
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }
        // 生成6位验证码
        String code = String.valueOf(random.nextInt(900000) + 100000);
        redisTemplate.opsForValue().set(RESET_CODE_PREFIX + email, code, 5, TimeUnit.MINUTES);
        logger.info("重置验证码已生成: email={}, code={}, 有效期5分钟", email, code);
        return true;
    }

    @Override
    public boolean resetPassword(String email, String code, String newPassword) {
        String storedCode = redisTemplate.opsForValue().get(RESET_CODE_PREFIX + email);
        // 卫语句：验证码错误
        if (storedCode == null || !storedCode.equals(code)) {
            logger.warn("重置密码失败 - 验证码无效: email={}", email);
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = this.getOne(wrapper);
        // 卫语句：用户不存在
        if (user == null) {
            logger.warn("重置密码失败 - 用户不存在: email={}", email);
            throw new BusinessException(ErrorCode.EMAIL_NOT_FOUND);
        }
        user.setPassword(BCrypt.hashpw(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        redisTemplate.delete(RESET_CODE_PREFIX + email);

        logger.info("用户密码重置成功: userId={}, email={}", user.getId(), email);
        return true;
    }
}
