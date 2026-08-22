package com.resume.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.platform.entity.SystemConfig;
import com.resume.platform.mapper.SystemConfigMapper;
import com.resume.platform.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置服务实现类
 * 注意：由于 MyBatis-Plus ServiceImpl 基类已定义 protected Log log，
 * 此处显式声明 SLF4J Logger 并命名为 logger，避免字段冲突且支持占位符参数
 *
 * @author system
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    /**
     * 默认配置项数量（用于HashMap初始容量，按负载因子换算）
     */
    private static final int DEFAULT_CONFIG_COUNT = 6;

    /**
     * getAllConfigs返回Map的初始容量（预留扩容空间）
     */
    private static final int ALL_CONFIGS_CAPACITY = 16;

    @Override
    public Map<String, String> getAllConfigs() {
        Map<String, String> result = new HashMap<>(ALL_CONFIGS_CAPACITY);
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        for (SystemConfig config : this.list(wrapper)) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        logger.debug("读取全部系统配置, 共{}项", result.size());
        return result;
    }

    @Override
    public String getConfigValue(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = this.getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public void updateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemConfig::getConfigKey, key);
            SystemConfig existing = this.getOne(wrapper);
            if (existing != null) {
                existing.setConfigValue(value);
                existing.setUpdateTime(LocalDateTime.now());
                this.updateById(existing);
                logger.debug("系统配置已更新: {} = {}", key, value);
            } else {
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(key);
                newConfig.setConfigValue(value);
                newConfig.setUpdateTime(LocalDateTime.now());
                this.save(newConfig);
                logger.debug("系统配置已新增: {} = {}", key, value);
            }
        }
    }

    @Override
    public void initDefaultConfigs() {
        String[][] defaults = {
            {"guest_can_view_home", "true", "游客是否可访问首页"},
            {"guest_can_view_resume", "true", "游客是否可查看简历"},
            {"guest_can_view_projects", "false", "游客是否可查看项目"},
            {"user_can_edit_resume", "true", "普通用户是否可编辑简历"},
            {"user_can_view_projects", "true", "普通用户是否可查看项目"},
            {"site_registration_enabled", "true", "是否允许新用户注册"}
        };

        int insertedCount = 0;
        for (String[] def : defaults) {
            LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemConfig::getConfigKey, def[0]);
            if (this.getOne(wrapper) == null) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(def[0]);
                config.setConfigValue(def[1]);
                config.setDescription(def[2]);
                config.setUpdateTime(LocalDateTime.now());
                this.save(config);
                insertedCount++;
            }
        }
        if (insertedCount > 0) {
            logger.info("系统默认配置初始化完成, 新增{}项（共{}项）", insertedCount, DEFAULT_CONFIG_COUNT);
        } else {
            logger.info("系统默认配置已全部存在, 跳过初始化");
        }
    }
}
