package com.resume.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 安全配置
 * 遵循阿里巴巴Java开发手册：
 * - 依赖注入使用构造器注入（@RequiredArgsConstructor）
 * - 集合初始化指定初始容量
 * - CORS使用allowedOriginPatterns代替allowedOrigins避免通配符+credentials冲突
 * - 鉴权入口/拒绝处理器异常保护，避免响应写入失败时抛出未捕获异常
 *
 * @author system
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 鉴权响应Result Map初始容量：code/message/data 约3个
     */
    private static final int RESPONSE_MAP_CAPACITY = 4;

    /**
     * CORS允许的HTTP方法
     */
    private static final List<String> CORS_ALLOWED_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
    );

    /**
     * CORS预检请求缓存时间（秒）
     */
    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    /**
     * JWT认证过滤器
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * JSON序列化工具（复用Spring容器已有的ObjectMapper更佳；此处手动实例化避免循环依赖）
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/public/admin-resume").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/system/config").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/system/config/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // AI对话接口：需要登录认证（JWT）
                .requestMatchers("/api/chat/**").authenticated()
                .requestMatchers("/api/project/upload").authenticated()
                .requestMatchers("/api/project/download/**").authenticated()
                .requestMatchers("/api/project/preview/**").permitAll()
                .requestMatchers("/api/system/config/**").hasRole("admin")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        log.info("Spring Security安全配置加载完成");
        return http.build();
    }

    /**
     * CORS跨域配置
     * 规范说明：必须使用addAllowedOriginPattern("*")+allowCredentials(true)组合
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(CORS_ALLOWED_METHODS);
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 未登录/登录过期 自定义响应入口
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            try {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                Map<String, Object> result = new HashMap<>(RESPONSE_MAP_CAPACITY);
                result.put("code", 401);
                result.put("message", "未登录或登录已过期，请重新登录");
                result.put("data", null);

                response.getWriter().write(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.error("写入401响应失败: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
    }

    /**
     * 无权限访问 自定义拒绝处理器
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            try {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                Map<String, Object> result = new HashMap<>(RESPONSE_MAP_CAPACITY);
                result.put("code", 403);
                result.put("message", "无权限访问该资源");
                result.put("data", null);

                response.getWriter().write(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.error("写入403响应失败: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
