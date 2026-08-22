package com.resume.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.resume.platform.entity.SystemConfig;

import java.util.Map;

/**
 * 系统配置服务接口
 *
 * @author system
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 查询所有系统配置（键值对形式）
     *
     * @return 配置Map：key -> value
     */
    Map<String, String> getAllConfigs();

    /**
     * 根据配置键查询配置值
     *
     * @param key 配置键
     * @return 配置值，不存在时返回null
     */
    String getConfigValue(String key);

    /**
     * 批量更新（或新增）系统配置
     * 对每个配置项：若已存在则更新值，不存在则新增记录
     *
     * @param configs 配置Map：key -> value
     */
    void updateConfigs(Map<String, String> configs);

    /**
     * 初始化默认系统配置
     * 仅在配置键不存在时插入，保证幂等性
     */
    void initDefaultConfigs();
}
