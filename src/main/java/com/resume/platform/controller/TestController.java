package com.resume.platform.controller;

import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.common.Result;
import com.resume.platform.dto.ProxyRequestDTO;
import com.resume.platform.dto.TestCaseSaveDTO;
import com.resume.platform.entity.TestCase;
import com.resume.platform.service.TestToolService;
import com.resume.platform.service.UserPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 测试工具控制器
 * 提供接口转发代理、网页代理、测试用例管理与执行、MD报告生成。
 * 所有端点需登录认证，并校验用户具备 test_tool 权限（默认允许）。
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin
@RequiredArgsConstructor
public class TestController {

    private static final String TEST_TOOL_PERMISSION = "test_tool";

    private final TestToolService testToolService;
    private final UserPermissionService userPermissionService;

    /**
     * 转发HTTP请求（接口测试）
     */
    @PostMapping("/proxy")
    public Result<Map<String, Object>> proxy(@Valid @RequestBody ProxyRequestDTO dto) {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.proxyRequest(dto));
    }

    /**
     * 代理外部网页（iframe可视化加载 + DOM检查）
     */
    @GetMapping("/proxy-page")
    public Result<Map<String, Object>> proxyPage(@RequestParam String url) {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.proxyPage(url));
    }

    /**
     * 保存测试用例
     */
    @PostMapping("/cases")
    public Result<TestCase> saveCase(@Valid @RequestBody TestCaseSaveDTO dto) {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.saveCase(userId, dto));
    }

    /**
     * 查询当前用户测试用例列表
     */
    @GetMapping("/cases")
    public Result<List<TestCase>> listCases() {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.listCases(userId));
    }

    /**
     * 删除测试用例
     */
    @DeleteMapping("/cases/{id}")
    public Result<Boolean> deleteCase(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.deleteCase(userId, id));
    }

    /**
     * 执行单条用例
     */
    @PostMapping("/cases/{id}/run")
    public Result<Map<String, Object>> runCase(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.runCase(userId, id));
    }

    /**
     * 批量执行全部用例并生成MD报告
     */
    @PostMapping("/run-all")
    public Result<Map<String, Object>> runAll() {
        Long userId = getCurrentUserId();
        checkTestToolPermission(userId);
        return Result.success(testToolService.runAllAndReport(userId));
    }

    /**
     * 查询当前用户是否具备测试工具权限（前端用于决定是否渲染入口）
     */
    @GetMapping("/my-access")
    public Result<Map<String, Boolean>> myAccess() {
        Long userId = getCurrentUserId();
        boolean allowed = userPermissionService.hasPermission(userId, TEST_TOOL_PERMISSION);
        return Result.success(Map.of("test_tool", allowed));
    }

    // ================== 私有工具方法 ==================

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) authentication.getDetails();
    }

    /**
     * 校验测试工具权限：默认允许，仅当被admin显式禁用才拒绝
     */
    private void checkTestToolPermission(Long userId) {
        if (!userPermissionService.hasPermission(userId, TEST_TOOL_PERMISSION)) {
            log.warn("测试工具权限被禁用: userId={}", userId);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
