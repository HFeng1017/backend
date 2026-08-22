package com.resume.platform.config;

import com.resume.platform.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 遵循阿里巴巴Java开发手册：
 * - 依赖注入使用构造器注入（@RequiredArgsConstructor + final字段）
 * - 异常捕获后必须记录日志（禁止空吞异常）
 * - 认证失败时清除上下文，避免残留信息被误用
 * - 魔法数字提取为常量（如Bearer前缀长度7）
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Authorization Header 前缀 Bearer + 空格 的长度
     */
    private static final int BEARER_PREFIX_LENGTH = 7;

    /**
     * Spring Security角色前缀
     */
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && !token.isEmpty()) {
                setAuthenticationFromToken(token);
            }
        } catch (Exception e) {
            // 规范要求：任何异常都必须清空认证上下文，避免脏数据
            SecurityContextHolder.clearContext();
            log.warn("JWT过滤器解析异常, uri={}, message={}", request.getRequestURI(), e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取token
     * 优先从Authorization Header取(Bearer方案)，其次从query参数取(支持浏览器直接预览文件)
     *
     * @param request HTTP请求
     * @return token字符串，找不到时返回null
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(BEARER_PREFIX_LENGTH).trim();
        }
        // 兼容 /api/project/preview/{id}?token=xxx 形式，便于新标签页直接预览
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isEmpty()) {
            log.debug("从query参数获取token: uri={}", request.getRequestURI());
            return queryToken;
        }
        return null;
    }

    /**
     * 根据有效token设置Spring Security认证上下文
     * 仅当token类型为access、且解析成功未过期时才设置认证信息
     *
     * @param token JWT字符串
     */
    private void setAuthenticationFromToken(String token) {
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null || jwtUtil.isTokenExpired(claims)) {
            return;
        }
        String tokenType = jwtUtil.getTokenType(claims);
        if (!"access".equals(tokenType)) {
            return;
        }
        Long userId = jwtUtil.getUserId(claims);
        String username = jwtUtil.getUsername(claims);
        String role = jwtUtil.getRole(claims);
        if (userId == null || username == null || role == null) {
            log.warn("JWT claims不完整: userId={}, username={}, role={}", userId, username, role);
            return;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(ROLE_PREFIX + role))
        );
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("JWT认证设置成功: userId={}, username={}, role={}", userId, username, role);
    }
}
