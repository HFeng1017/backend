package com.resume.platform.config;

import cn.hutool.crypto.digest.BCrypt;
import com.resume.platform.entity.Resume;
import com.resume.platform.entity.User;
import com.resume.platform.service.ResumeService;
import com.resume.platform.service.SystemConfigService;
import com.resume.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动数据初始化器
 * 遵循阿里巴巴Java开发手册：
 * - 禁止使用System.out.println输出日志，统一使用log框架
 * - 依赖注入使用构造器注入（@RequiredArgsConstructor）
 * - 幂等性：先查再插，避免每次重启重建数据导致ID变化
 * - 卫语句：判空后再执行创建，减少嵌套层级
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    /**
     * 账号正常状态
     */
    private static final int STATUS_ENABLED = 1;

    /**
     * Admin默认用户名
     */
    private static final String ADMIN_USERNAME = "admin";

    /**
     * 普通测试用户用户名
     */
    private static final String TEST_USERNAME = "user";

    /**
     * Admin默认密码
     */
    private static final String ADMIN_DEFAULT_PASSWORD = "admin123";

    /**
     * 测试用户默认密码
     */
    private static final String TEST_DEFAULT_PASSWORD = "user123";

    private final UserService userService;
    private final ResumeService resumeService;
    private final SystemConfigService systemConfigService;

    static {
        // 类加载时预生成两个默认密码的BCrypt哈希，打印到日志便于远程DB手动UPDATE
        // BCrypt 每次生成的盐值不同，但 checkpw 都能验证
        String h1 = BCrypt.hashpw(ADMIN_DEFAULT_PASSWORD);
        String h2 = BCrypt.hashpw(TEST_DEFAULT_PASSWORD);
        System.out.println("==== BCrypt Hash Debug ====");
        System.out.println("admin123 => " + h1);
        System.out.println("user123  => " + h2);
        System.out.println("============================");
    }

    @Override
    public void run(String... args) {
        log.info("==================== 开始执行启动数据初始化 ====================");

        // 1. 初始化管理员用户（仅当不存在时创建，保证ID稳定）
        User adminUser = initAdminUser();

        // 2. 为管理员初始化示例简历（仅当不存在时创建）
        initAdminResume(adminUser);

        // 3. 初始化普通测试用户
        initTestUser();

        // 4. 初始化系统权限配置
        systemConfigService.initDefaultConfigs();
        log.info("系统权限配置初始化完成");

        log.info("==================== 启动数据初始化执行完毕 ====================");
    }

    /**
     * 初始化管理员用户
     *
     * @return 管理员用户实体
     */
    private User initAdminUser() {
        User adminUser = userService.getUserByUsername(ADMIN_USERNAME);
        if (adminUser != null && isValidBcryptHash(adminUser.getPassword())) {
            log.info("管理员用户已存在且密码有效(username={}), 跳过创建", ADMIN_USERNAME);
            return adminUser;
        }
        if (adminUser != null) {
            // 密码被 schema.sql 占位符污染，强制重置为真实 BCrypt 哈希
            // 必须用 updateById 而非 save：save() 对有 ID 的实体会 INSERT 撞主键
            log.warn("管理员用户存在但密码非有效BCrypt哈希，重置密码(username={})", ADMIN_USERNAME);
            adminUser.setPassword(BCrypt.hashpw(ADMIN_DEFAULT_PASSWORD));
            adminUser.setStatus(STATUS_ENABLED);
            userService.updateById(adminUser);
            log.info("管理员密码已重置: username={}, password={}", ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
            return adminUser;
        }
        adminUser = new User();
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setPassword(BCrypt.hashpw(ADMIN_DEFAULT_PASSWORD));
        adminUser.setEmail("admin@example.com");
        adminUser.setPhone("13800138000");
        adminUser.setRole(ADMIN_USERNAME);
        adminUser.setStatus(STATUS_ENABLED);
        userService.save(adminUser);
        log.info("管理员用户创建成功: username={}, password={}", ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
        return adminUser;
    }

    /**
     * 判断密码是否为有效的 BCrypt 哈希
     * BCrypt 哈希特征：60字符，以 $2a$ / $2b$ / $2y$ 开头
     */
    private static boolean isValidBcryptHash(String password) {
        if (password == null) return false;
        return password.length() == 60
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    /**
     * 为admin用户初始化示例简历（游客模式展示用）
     *
     * @param adminUser 管理员用户实体
     */
    private void initAdminResume(User adminUser) {
        if (resumeService.getResumeByUserId(adminUser.getId()) != null) {
            log.info("管理员用户简历已存在(userId={}), 跳过创建", adminUser.getId());
            return;
        }
        Resume adminResume = new Resume();
        adminResume.setUserId(adminUser.getId());
        adminResume.setName("胡礼枫");
        adminResume.setTitle("全栈开发工程师");
        adminResume.setAvatar("");
        adminResume.setEmail(adminUser.getEmail());
        adminResume.setPhone(adminUser.getPhone());
        adminResume.setIntroduction("我独立从0到1开发并部署的前后端分离个人博客项目，最能体现我的\"全链路能力\"。该项目中，我不仅使用SpringBoot和Vue3完成了核心功能开发，还独立编写了自动化测试脚本保障质量，并通过Docker容器化部署及Nginx配置实现了生产环境上线，完整闭环了\"开发-测试-部署-运维\"流程。");
        adminResume.setSkills("Vue3,React,Spring Boot,MyBatis Plus,MySQL,Redis,Docker,Nginx");
        adminResume.setExperience("广西斯蓝信息技术有限公司 | 2023.06 - 2023.12\n职位：Java开发工程师\n描述：技术栈 Springboot、SpringMVC、Vue3、MySQL。负责内部产品商城、资金存取、仓库入库等模块的单元测试与集成测试，编写JUnit测试用例，提升模块代码覆盖率。");
        adminResume.setEducation("广西科技师范学院 | 2024.09 - 2026.06\n专业：数据科学与大数据技术 · 本科");
        adminResume.setProjects("项目名称：个人简历管理系统\n描述：用于管理个人简历的系统，支持简历编辑、预览、导出等功能，采用前后端分离架构开发。");
        adminResume.setStatus(STATUS_ENABLED);
        adminResume.setCreateTime(LocalDateTime.now());
        adminResume.setUpdateTime(LocalDateTime.now());
        resumeService.save(adminResume);
        log.info("管理员示例简历创建成功: userId={}", adminUser.getId());
    }

    /**
     * 初始化普通测试用户
     */
    private void initTestUser() {
        User user = userService.getUserByUsername(TEST_USERNAME);
        if (user != null && isValidBcryptHash(user.getPassword())) {
            log.info("测试用户已存在且密码有效(username={}), 跳过创建", TEST_USERNAME);
            return;
        }
        if (user != null) {
            log.warn("测试用户存在但密码非有效BCrypt哈希，重置密码(username={})", TEST_USERNAME);
            user.setPassword(BCrypt.hashpw(TEST_DEFAULT_PASSWORD));
            user.setStatus(STATUS_ENABLED);
            userService.updateById(user);
            log.info("测试用户密码已重置: username={}, password={}", TEST_USERNAME, TEST_DEFAULT_PASSWORD);
            return;
        }
        user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(BCrypt.hashpw(TEST_DEFAULT_PASSWORD));
        user.setEmail("user@example.com");
        user.setPhone("13800138001");
        user.setRole(TEST_USERNAME);
        user.setStatus(STATUS_ENABLED);
        userService.save(user);
        log.info("测试用户创建成功: username={}, password={}", TEST_USERNAME, TEST_DEFAULT_PASSWORD);
    }
}
