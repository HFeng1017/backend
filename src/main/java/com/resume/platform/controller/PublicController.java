package com.resume.platform.controller;

import com.resume.platform.common.Result;
import com.resume.platform.dto.ResumeSearchVO;
import com.resume.platform.entity.Resume;
import com.resume.platform.entity.User;
import com.resume.platform.service.ResumeService;
import com.resume.platform.service.SystemConfigService;
import com.resume.platform.service.UserService;
import com.resume.platform.utils.RsaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公共（游客可访问）控制器
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@CrossOrigin
@RequiredArgsConstructor
public class PublicController {

    private final ResumeService resumeService;
    private final UserService userService;
    private final SystemConfigService systemConfigService;
    private final RsaUtil rsaUtil;

    /**
     * 轮播图数组长度
     */
    private static final int BANNER_COUNT = 3;

    /**
     * 权限配置Map初始容量：5个权限项
     */
    private static final int PERMISSIONS_CAPACITY = 8;

    /**
     * 公共首页数据（轮播图占位）
     */
    @GetMapping("/home")
    public Result<Map<String, Object>> getHomeData() {
        Map<String, Object> data = new HashMap<>(4);
        String[] banners = new String[BANNER_COUNT];
        banners[0] = "https://picsum.photos/800/300?random=1";
        banners[1] = "https://picsum.photos/800/300?random=2";
        banners[2] = "https://picsum.photos/800/300?random=3";
        data.put("banners", banners);
        return Result.success(data);
    }

    /**
     * 获取系统权限配置（游客、普通用户的访问开关）
     */
    @GetMapping("/permissions")
    public Result<Map<String, String>> getPermissions() {
        Map<String, String> configs = systemConfigService.getAllConfigs();
        Map<String, String> permissions = new HashMap<>(PERMISSIONS_CAPACITY);
        permissions.put("guest_can_view_home", configs.getOrDefault("guest_can_view_home", "true"));
        permissions.put("guest_can_view_resume", configs.getOrDefault("guest_can_view_resume", "true"));
        permissions.put("guest_can_view_projects", configs.getOrDefault("guest_can_view_projects", "false"));
        permissions.put("user_can_view_projects", configs.getOrDefault("user_can_view_projects", "true"));
        permissions.put("user_can_edit_resume", configs.getOrDefault("user_can_edit_resume", "true"));
        return Result.success(permissions);
    }

    /**
     * 获取admin用户的示例简历（游客模式展示用）
     * 通过用户名查询admin，避免硬编码用户ID
     */
    @GetMapping("/admin-resume")
    public Result<Resume> getAdminResume() {
        User admin = userService.getUserByUsername("admin");
        if (admin == null) {
            return Result.success(null);
        }
        Resume resume = resumeService.getResumeByUserId(admin.getId());
        log.debug("游客模式获取admin简历: adminUserId={}", admin.getId());
        return Result.success(resume);
    }

    /**
     * 简历搜索（游客可访问）
     *
     * 支持两种场景：
     * 1. 精准搜索：输入简历姓名，全等匹配命中排最前（得分100）
     * 2. 模糊搜索：输入"Java工程师"、"全栈工程师"等关键词，
     *    FULLTEXT ngram 分词召回职位/技能/简介匹配的简历，按相关性降序
     *
     * @param keyword 关键词（姓名或职位/技能）
     * @param page    页码，默认1
     * @param size    每页条数，默认10，上限20
     * @return 搜索结果（含相关性得分与命中字段）
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        List<ResumeSearchVO> results = resumeService.searchResumes(keyword, page, size);
        long total = resumeService.countSearchResults(keyword);

        Map<String, Object> data = new HashMap<>(8);
        data.put("keyword", keyword == null ? "" : keyword.trim());
        data.put("page", Math.max(page, 1));
        data.put("size", size);
        data.put("total", total);
        data.put("results", results);
        return Result.success(data);
    }

    /**
     * 根据用户ID查看公开简历
     */
    @GetMapping("/resume/{userId}")
    public Result<Resume> getPublicResume(@PathVariable Long userId) {
        Resume resume = resumeService.getResumeByUserId(userId);
        return Result.success(resume);
    }

    /**
     * 获取 RSA 公钥（PEM格式）
     * 前端登录前调用，用于对用户输入的密码进行公钥加密
     */
    @GetMapping("/public-key")
    public Result<Map<String, String>> getRsaPublicKey() {
        String publicKeyPem = rsaUtil.getPublicKeyPem();
        Map<String, String> data = new HashMap<>(4);
        data.put("publicKey", publicKeyPem);
        log.debug("下发RSA公钥: pem长度={}", publicKeyPem.length());
        return Result.success(data);
    }
}
