package com.resume.platform.controller;

import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.common.Result;
import com.resume.platform.dto.SystemConfigUpdateDTO;
import com.resume.platform.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置控制器
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/system/config")
@CrossOrigin
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 系统配置项Map初始容量
     */
    private static final int CONFIG_MAP_CAPACITY = 8;

    /**
     * 查询所有配置（GET公开查询权限，供游客模式/前端初始化读取）
     */
    @GetMapping
    public Result<Map<String, String>> getAllConfigs() {
        log.debug("读取系统配置");
        return Result.success(systemConfigService.getAllConfigs());
    }

    /**
     * 更新系统配置 —— 只有admin角色可调用
     * 安全规约：操作前校验当前登录者为admin角色，避免越权篡改系统配置
     */
    @PostMapping
    public Result<String> updateConfigs(@Valid @RequestBody SystemConfigUpdateDTO dto) {
        if (!isAdminUser()) {
            log.warn("越权修改系统配置已拦截，当前登录者非admin");
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        Map<String, String> configs = new HashMap<>(CONFIG_MAP_CAPACITY);
        if (dto.getGuestCanViewHome() != null) {
            configs.put("guest_can_view_home", dto.getGuestCanViewHome());
        }
        if (dto.getGuestCanViewResume() != null) {
            configs.put("guest_can_view_resume", dto.getGuestCanViewResume());
        }
        if (dto.getGuestCanViewProjects() != null) {
            configs.put("guest_can_view_projects", dto.getGuestCanViewProjects());
        }
        if (dto.getUserCanEditResume() != null) {
            configs.put("user_can_edit_resume", dto.getUserCanEditResume());
        }
        if (dto.getUserCanViewProjects() != null) {
            configs.put("user_can_view_projects", dto.getUserCanViewProjects());
        }
        if (dto.getSiteRegistrationEnabled() != null) {
            configs.put("site_registration_enabled", dto.getSiteRegistrationEnabled());
        }
        systemConfigService.updateConfigs(configs);
        log.info("系统配置已更新: configKeys={}", configs.keySet());
        return Result.success("配置更新成功");
    }

    /**
     * 判断当前登录用户是否是admin角色
     */
    private boolean isAdminUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> "ROLE_admin".equals(granted.getAuthority()));
    }
}
