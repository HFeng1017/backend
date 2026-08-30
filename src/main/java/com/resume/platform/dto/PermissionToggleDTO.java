package com.resume.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限开关 DTO
 * admin 给指定用户开关某项权限。
 *
 * @author system
 */
@Data
public class PermissionToggleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 权限标识：test_tool/project_view/resume_edit/chat_use
     */
    @NotBlank(message = "权限标识不能为空")
    private String permissionKey;

    /**
     * 是否启用：true-启用 false-禁用
     */
    @NotNull(message = "开关状态不能为空")
    private Boolean enabled;
}
