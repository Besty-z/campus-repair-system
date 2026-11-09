package com.campus.repair.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类：负责签发与解析登录令牌（HS256，有效期默认 7 天）。
 *
 * @author campus
 */
@Component
public class JwtUtils {

    /** 签名密钥（HS256 要求至少 32 字节） */
    private final Key key;

    /** 有效期（毫秒） */
    private final long expireMillis;

    /**
     * 构造 JWT 工具类。
     *
     * @param secret     密钥字符串，来自配置 jwt.secret
     * @param expireDays 有效期天数，来自配置 jwt.expire-days
     */
    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expire-days:7}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireDays * 24L * 60L * 60L * 1000L;
    }

    /**
     * 签发登录令牌：subject 存放用户ID，自定义 claim 存放角色。
     *
     * @param userId 用户ID
     * @param role   用户角色
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并校验令牌。
     *
     * @param token JWT 字符串
     * @return 令牌中的 Claims（subject 为用户ID，role 为角色）
     * @throws io.jsonwebtoken.JwtException 令牌非法或已过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
