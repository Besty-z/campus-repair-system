package com.campus.repair.common;

import java.io.Serializable;

/**
 * 统一响应体，结构为 {code, message, data}。
 *
 * @param <T> 业务数据类型
 * @author campus
 */
public class Result<T> implements Serializable {

    /** 状态码：200 成功，其余为各类失败 */
    private Integer code;

    /** 提示信息，失败时为可直接展示给用户的中文文案 */
    private String message;

    /** 业务数据 */
    private T data;

    /**
     * 无参构造器，供 Jackson 序列化使用。
     */
    public Result() {
    }

    /**
     * 全参构造器。
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    业务数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造成功响应（message 默认为 success）。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功响应体
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS, "success", data);
    }

    /**
     * 构造带自定义提示信息的成功响应。
     *
     * @param message 提示信息
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 成功响应体
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS, message, data);
    }

    /**
     * 构造失败响应。
     *
     * @param code    失败状态码
     * @param message 失败提示信息
     * @param <T>     业务数据类型
     * @return 失败响应体
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 设置状态码。
     *
     * @param code 状态码
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * 获取提示信息。
     *
     * @return 提示信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置提示信息。
     *
     * @param message 提示信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取业务数据。
     *
     * @return 业务数据
     */
    public T getData() {
        return data;
    }

    /**
     * 设置业务数据。
     *
     * @param data 业务数据
     */
    public void setData(T data) {
        this.data = data;
    }
}
