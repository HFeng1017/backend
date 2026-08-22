package com.resume.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 * 主要功能：配置上传目录的静态资源映射，使 /uploads/** 可以直接访问用户上传的文件
 * 遵循阿里巴巴Java开发手册：
 * - 文件路径统一使用File.separator避免跨平台差异
 * - 配置类添加日志，便于启动时排查映射是否生效
 *
 * @author system
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 静态资源访问路径前缀
     */
    private static final String RESOURCE_HANDLER_PATTERN = "/uploads/**";

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = new File(uploadDir).getAbsolutePath();
        String resourceLocation = "file:" + absolutePath + File.separator;
        registry.addResourceHandler(RESOURCE_HANDLER_PATTERN)
                .addResourceLocations(resourceLocation);
        log.info("上传文件静态资源映射已注册: pattern={}, location={}", RESOURCE_HANDLER_PATTERN, resourceLocation);
    }
}
