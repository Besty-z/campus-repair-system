package com.campus.repair.vo;

import java.io.Serializable;

/**
 * 发送验证码返回对象。
 *
 * @author campus
 */
public class SendCodeVO implements Serializable {

    /** 验证码有效期（秒） */
    private Integer expireSeconds;

    /**
     * 无参构造器。
     */
    public SendCodeVO() {
    }

    /**
     * 全参构造器。
     *
     * @param expireSeconds 验证码有效期（秒）
     */
    public SendCodeVO(Integer expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    /**
     * 获取验证码有效期。
     *
     * @return 验证码有效期（秒）
     */
    public Integer getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 设置验证码有效期。
     *
     * @param expireSeconds 验证码有效期（秒）
     */
    public void setExpireSeconds(Integer expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
