package com.campus.repair.common;

/**
 * 业务异常：携带业务状态码与中文提示信息，由全局异常处理器统一转换为 {@link Result}。
 *
 * @author campus
 */
public class BizException extends RuntimeException {

    /** 业务状态码（400/401/403/500 等） */
    private final int code;

    /**
     * 构造业务异常。
     *
     * @param code    业务状态码
     * @param message 可直接提示给用户的中文文案
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public int getCode() {
        return code;
    }
}
