package com.resume.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.entity.User;
import com.resume.platform.entity.UserPermission;
import com.resume.platform.mapper.UserMapper;
import com.resume.platform.mapper.UserPermissionMapper;
import com.resume.platform.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户级权限服务实现
 * admin 按用户名/ID搜索用户，并开关其功能权限。
 * 权限规则：未配置默认允许；配置为禁用才拒绝。
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    /**
     * 默认允许（未配置项视为启用）
     */
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    /**
     * 可管理的权限项目录（供前端展示）
     */
    private static final Map<String, String> PERMISSION_CATALOG = new LinkedHashMap<>();

    static {
        PERMISSION_CATALOG.put("test_tool", "测试工具访问");
        PERMISSION_CATALOG.put("project_view", "项目查看");
        PERMISSION_CATALOG.put("resume_edit", "简历编辑");
        PERMISSION_CATALOG.put("chat_use", "AI对话使用");
    }

    private final UserMapper userMapper;
    private final UserPermissionMapper userPermissionMapper;

    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "搜索关键词不能为空");
        }
        String kw = keyword.trim();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 纯数字按ID精确匹配，否则按用户名模糊
        if (kw.matches("\\d+")) {
            wrapper.eq(User::getId, Long.parseLong(kw))
                    .or()
                    .like(User::getUsername, kw);
        } else {
            wrapper.like(User::getUsername, kw);
        }
        List<User> users = userMapper.selectList(wrapper);
        // 脱敏：清除密码字段
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public Map<String, Boolean> getUserPermissions(Long userId) {
        LambdaQueryWrapper<UserPermission> wrapper = new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, userId);
        List<UserPermission> list = userPermissionMapper.selectList(wrapper);
        Map<String, Boolean> result = new LinkedHashMap<>();
        // 目录中所有项默认启用
        PERMISSION_CATALOG.forEach((key, desc) -> result.put(key, true));
        // 用数据库实际配置覆盖
        for (UserPermission p : list) {
            result.put(p.getPermissionKey(), p.getEnabled() != null && p.getEnabled() == ENABLED);
        }
        return result;
    }

    @Override
    public UserPermission togglePermission(Long userId, String permissionKey, Boolean enabled) {
        // 校验用户存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        // 校验权限标识合法
        if (!PERMISSION_CATALOG.containsKey(permissionKey)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知权限标识: " + permissionKey);
        }
        LambdaQueryWrapper<UserPermission> wrapper = new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermissionKey, permissionKey);
        UserPermission existing = userPermissionMapper.selectOne(wrapper);
        if (existing == null) {
            existing = new UserPermission();
            existing.setUserId(userId);
            existing.setPermissionKey(permissionKey);
            existing.setEnabled(enabled ? ENABLED : DISABLED);
            existing.setCreateTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            userPermissionMapper.insert(existing);
        } else {
            existing.setEnabled(enabled ? ENABLED : DISABLED);
            existing.setUpdateTime(LocalDateTime.now());
            userPermissionMapper.updateById(existing);
        }
        log.info("权限开关已更新: userId={}, key={}, enabled={}", userId, permissionKey, enabled);
        // 返回时附带描述
        return existing;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionKey) {
        LambdaQueryWrapper<UserPermission> wrapper = new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermissionKey, permissionKey);
        UserPermission p = userPermissionMapper.selectOne(wrapper);
        if (p == null) {
            // 未配置默认允许
            return true;
        }
        return p.getEnabled() != null && p.getEnabled() == ENABLED;
    }

    /**
     * 权限目录（供控制器暴露给前端渲染开关列表）
     */
    public static Map<String, String> getPermissionCatalog() {
        return PERMISSION_CATALOG;
    }
}
