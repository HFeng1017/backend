package com.resume.platform.controller;

import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.common.Result;
import com.resume.platform.dto.PermissionToggleDTO;
import com.resume.platform.entity.User;
import com.resume.platform.entity.UserPermission;
import com.resume.platform.service.UserPermissionService;
import com.resume.platform.service.impl.UserPermissionServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户权限管理控制器
 * 仅 admin 可访问。支持按用户名/ID搜索用户，并开关其功能权限。
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;

    /**
     * 按用户名或ID搜索用户（脱敏返回）
     */
    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        requireAdmin();
        return Result.success(userPermissionService.searchUsers(keyword));
    }

    /**
     * 获取指定用户权限开关
     */
    @GetMapping("/{userId}/permissions")
    public Result<Map<String, Boolean>> getPermissions(@PathVariable Long userId) {
        requireAdmin();
        return Result.success(userPermissionService.getUserPermissions(userId));
    }

    /**
     * 开关用户权限
     */
    @PostMapping("/permissions")
    public Result<UserPermission> togglePermission(@Valid @RequestBody PermissionToggleDTO dto) {
        requireAdmin();
        return Result.success(userPermissionService.togglePermission(
                dto.getUserId(), dto.getPermissionKey(), dto.getEnabled()));
    }

    /**
     * 权限目录（供前端渲染开关列表）
     */
    @GetMapping("/permissions/catalog")
    public Result<Map<String, String>> catalog() {
        requireAdmin();
        return Result.success(UserPermissionServiceImpl.getPermissionCatalog());
    }

    // ================== 私有工具方法 ==================

    /**
     * 校验当前登录用户为 admin 角色
     */
    private void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        boolean isAdmin = authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(g -> "ROLE_admin".equals(g.getAuthority()));
        if (!isAdmin) {
            log.warn("非admin访问权限管理接口: name={}", authentication.getName());
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
