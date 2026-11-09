package com.campus.repair.common;

/**
 * 当前登录用户上下文：基于 ThreadLocal 保存 userId 与 role，
 * 由 JWT 拦截器在请求进入时写入、请求结束后清除。
 *
 * @author campus
 */
public final class UserContext {

    /** 当前线程的用户ID */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /** 当前线程的用户角色 */
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private UserContext() {
    }

    /**
     * 写入当前登录用户信息。
     *
     * @param userId 用户ID
     * @param role   用户角色
     */
    public static void set(Long userId, String role) {
        USER_ID_HOLDER.set(userId);
        ROLE_HOLDER.set(role);
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 用户ID，未登录时返回 null
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前登录用户角色。
     *
     * @return 用户角色，未登录时返回 null
     */
    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    /**
     * 清除当前线程的用户信息（必须调用，防止线程复用导致的数据串号）。
     */
    public static void remove() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
