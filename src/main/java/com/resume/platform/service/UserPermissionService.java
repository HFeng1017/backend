package com.resume.platform.service;

import com.resume.platform.entity.User;
import com.resume.platform.entity.UserPermission;

import java.util.List;
import java.util.Map;

/**
 * 用户级权限服务
 * admin 可按用户名/ID搜索用户，并开关其功能权限。
 *
 * @author system
 */
public interface UserPermissionService {

    /**
     * 按用户名或ID搜索用户
     *
     * @param keyword 关键词（用户名模糊 或 纯数字ID精确）
     * @return 用户列表（脱敏：不含密码）
     */
    List<User> searchUsers(String keyword);

    /**
     * 获取用户权限开关
     *
     * @param userId 用户ID
     * @return 权限标识 -> 是否启用（未配置项默认视为启用）
     */
    Map<String, Boolean> getUserPermissions(Long userId);

    /**
     * 开关用户权限（存在则更新，不存在则新增）
     *
     * @param userId        用户ID
     * @param permissionKey 权限标识
     * @param enabled       是否启用
     * @return 更新后的权限
     */
    UserPermission togglePermission(Long userId, String permissionKey, Boolean enabled);

    /**
     * 校验用户是否拥有某权限
     * 规则：未配置默认允许（true）；配置为禁用则拒绝（false）
     *
     * @param userId        用户ID
     * @param permissionKey 权限标识
     * @return true-允许 false-拒绝
     */
    boolean hasPermission(Long userId, String permissionKey);
}
