package com.resume.platform.controller;

import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.common.Result;
import com.resume.platform.dto.ResumeSaveDTO;
import com.resume.platform.entity.Resume;
import com.resume.platform.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 简历控制器
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@CrossOrigin
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 根据用户ID查询简历
     * 说明：此接口公开使用（用户可看他人公开简历），不做越权限制
     *
     * @param userId 用户ID
     * @return 简历数据
     */
    @GetMapping("/{userId}")
    public Result<Resume> getResume(@PathVariable Long userId) {
        log.info("查询简历: userId={}", userId);
        Resume resume = resumeService.getResumeByUserId(userId);
        return Result.success(resume);
    }

    /**
     * 保存（创建或更新）当前用户的简历
     * 安全规约：必须校验 dto.getUserId() 与当前登录用户一致，防止越权篡改他人简历
     *
     * @param dto 简历提交数据
     * @return 保存后的简历
     */
    @PostMapping
    public Result<Resume> saveResume(@Valid @RequestBody ResumeSaveDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("保存简历: 登录userId={}, 请求userId={}", currentUserId, dto.getUserId());

        // 越权校验：admin可修改任意人的简历；普通用户只能修改自己的
        if (!isAdminUser() && !currentUserId.equals(dto.getUserId())) {
            log.warn("越权修改简历拦截: 登录userId={}, 请求userId={}", currentUserId, dto.getUserId());
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        Resume resume = new Resume();
        resume.setUserId(dto.getUserId());
        resume.setName(dto.getName());
        resume.setTitle(dto.getTitle());
        resume.setAvatar(dto.getAvatar());
        resume.setEmail(dto.getEmail());
        resume.setPhone(dto.getPhone());
        resume.setIntroduction(dto.getIntroduction());
        resume.setSkills(dto.getSkills());
        resume.setExperience(dto.getExperience());
        resume.setEducation(dto.getEducation());
        resume.setProjects(dto.getProjects());
        Resume saved = resumeService.createOrUpdateResume(resume);
        return Result.success(saved);
    }

    /**
     * 从Spring Security上下文获取当前登录用户ID
     * 未登录时抛出未授权业务异常
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
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
