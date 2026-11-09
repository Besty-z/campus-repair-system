package com.campus.repair.common;

/**
 * 统一返回码常量。
 * 与接口契约约定一致：200 成功，400 参数/业务校验失败，401 未登录，403 无权限，500 服务器异常。
 *
 * @author campus
 */
public final class ResultCode {

    /** 成功 */
    public static final int SUCCESS = 200;

    /** 参数错误或业务校验失败 */
    public static final int BAD_REQUEST = 400;

    /** 未登录或登录已过期 */
    public static final int UNAUTHORIZED = 401;

    /** 无权限访问 */
    public static final int FORBIDDEN = 403;

    /** 服务器内部异常 */
    public static final int SERVER_ERROR = 500;

    /**
     * 工具类不允许实例化。
     */
    private ResultCode() {
    }
}
