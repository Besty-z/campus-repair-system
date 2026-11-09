package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 验证码登录请求对象。
 *
 * @author campus
 */
public class LoginDTO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;

    /**
     * 无参构造器。
     */
    public LoginDTO() {
    }

    /**
     * 获取手机号。
     *
     * @return 手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置手机号。
     *
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取短信验证码。
     *
     * @return 短信验证码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置短信验证码。
     *
     * @param code 短信验证码
     */
    public void setCode(String code) {
        this.code = code;
    }
}
