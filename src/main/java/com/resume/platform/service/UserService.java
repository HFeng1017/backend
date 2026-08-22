package com.resume.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.resume.platform.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 *
 * @author system
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询单个用户
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回null
     */
    User getUserByUsername(String username);

    /**
     * 用户登录，返回 accessToken / refreshToken
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @return Token集合Map: accessToken, refreshToken
     * @throws com.resume.platform.common.BusinessException 用户名不存在、密码错误、账号禁用
     */
    Map<String, String> login(String username, String password);

    /**
     * 使用 refreshToken 换取新的 accessToken
     *
     * @param refreshToken 刷新令牌
     * @return 新的 accessToken
     */
    String refreshToken(String refreshToken);

    /**
     * 发送重置密码验证码（演示版实际存Redis，打印日志模拟邮件）
     *
     * @param email 用户邮箱
     * @return 发送成功标志
     */
    boolean sendResetCode(String email);

    /**
     * 校验验证码并重置密码
     *
     * @param email       用户邮箱
     * @param code        验证码
     * @param newPassword 新密码（明文）
     * @return 是否重置成功
     */
    boolean resetPassword(String email, String code, String newPassword);
}
