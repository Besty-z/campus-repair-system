package com.campus.repair.interceptor;

import com.campus.repair.common.UserContext;
import com.campus.repair.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 登录鉴权拦截器：
 * 校验 Authorization 请求头中的 Bearer 令牌，合法则把 userId/role 写入 {@link UserContext}，
 * 请求结束后负责清理 ThreadLocal。
 *
 * @author campus
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    /** Bearer 令牌前缀 */
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    /**
     * 构造拦截器。
     *
     * @param jwtUtils JWT 工具类
     */
    public JwtInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /**
     * 请求进入前校验令牌。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return true 放行；false 拦截（已写入 401 响应）
     * @throws IOException 写出响应失败时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // CORS 预检请求直接放行（预检不携带 Authorization 头）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            String token = header.substring(TOKEN_PREFIX.length());
            try {
                Claims claims = jwtUtils.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);
                UserContext.set(userId, role);
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT 校验失败: {}", e.getMessage());
            }
        }
        writeUnauthorized(response);
        return false;
    }

    /**
     * 请求完成后清理 ThreadLocal，防止线程复用导致数据串号。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       处理过程中的异常（可能为 null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.remove();
    }

    /**
     * 写出 401 未登录响应。
     *
     * @param response HTTP 响应
     * @throws IOException 写出失败时抛出
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
    }
}
