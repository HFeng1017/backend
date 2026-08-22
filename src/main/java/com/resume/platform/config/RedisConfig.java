package com.resume.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 客户端配置
 * 遵循阿里巴巴Java开发手册：
 * - RedisTemplate泛型必须指定具体类型（禁止RedisTemplate<Object, Object>裸用）
 * - 序列化策略必须明确指定，避免默认JDK序列化导致的兼容性/性能问题
 * - 配置类使用构造器参数注入RedisConnectionFactory（Spring自动注入）
 *
 * @author system
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate，统一使用String序列化方案
     * 适用场景：本项目Redis主要存字符串（重置验证码、Token黑名单等）
     *
     * @param connectionFactory Redis连接工厂（Spring Boot自动装配）
     * @return RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        log.info("RedisTemplate初始化完成(序列化: StringRedisSerializer)");
        return template;
    }
}
