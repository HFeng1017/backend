package com.resume.platform.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 遵循阿里巴巴Java开发手册：
 * - 时间类优先使用 Instant/LocalDateTime 替代 Date
 * - Token的claims Map指定初始容量
 * - 异常禁止空吞，保留原始抛出点信息
 *
 * @author system
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * Claims固定字段数：userId/username/role/type，外加预留空间
     */
    private static final int CLAIMS_MAP_CAPACITY = 8;

    /**
     * Token类型-访问令牌
     */
    private static final String TOKEN_TYPE_ACCESS = "access";

    /**
     * Token类型-刷新令牌
     */
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private SecretKey secretKey;

    @Value("${jwt.access-token-expire}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-token-expire}")
    private Long refreshTokenExpire;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS512算法建议密钥长度>=64字节，不足则用安全方法生成
        if (keyBytes.length < 64) {
            this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            log.warn("JWT密钥长度不足64字节，已自动生成安全密钥。生产环境请配置长度>=64的jwt.secret");
        } else {
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * 生成访问令牌（短有效期）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return JWT字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>(CLAIMS_MAP_CAPACITY);
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", TOKEN_TYPE_ACCESS);
        return generateToken(claims, accessTokenExpire);
    }

    /**
     * 生成刷新令牌（长有效期）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return JWT字符串
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>(CLAIMS_MAP_CAPACITY);
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", TOKEN_TYPE_REFRESH);
        return generateToken(claims, refreshTokenExpire);
    }

    /**
     * 实际生成JWT令牌
     * 说明：JJWT 0.11.x API仍然要求使用Date参数，但统一用Instant.ofEpochMilli做换算，
     * 保证业务侧使用现代化时间API，避免Date潜在问题。
     */
    private String generateToken(Map<String, Object> claims, long expireMs) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expireMs);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 解析Token并返回Claims
     * 注：ExpiredJwtException时仍然返回过期的claims，便于业务判断刷新逻辑
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("JWT已过期: subject={}", e.getClaims().getSubject());
            return e.getClaims();
        } catch (Exception e) {
            log.debug("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断claims是否过期
     */
    public boolean isTokenExpired(Claims claims) {
        if (claims == null) {
            return true;
        }
        Instant expire = claims.getExpiration().toInstant();
        return expire.isBefore(Instant.now());
    }

    public Long getUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getTokenType(Claims claims) {
        return claims.get("type", String.class);
    }

    /**
     * 综合校验：解析成功且未过期
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.warn("JWT综合校验失败: {}", e.getMessage());
            return false;
        }
    }
}
