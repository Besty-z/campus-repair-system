package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 短信验证码实体，对应表 sms_code。
 * 课程作业采用模拟短信，验证码打印在后端日志中。
 *
 * @author campus
 */
@TableName("sms_code")
public class SmsCode {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号 */
    private String phone;

    /** 验证码 */
    private String code;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 是否已使用：1是 0否 */
    private Integer used;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 无参构造器。
     */
    public SmsCode() {
    }

    /**
     * 获取主键ID。
     *
     * @return 主键ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键ID。
     *
     * @param id 主键ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * 获取验证码。
     *
     * @return 验证码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置验证码。
     *
     * @param code 验证码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取过期时间。
     *
     * @return 过期时间
     */
    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    /**
     * 设置过期时间。
     *
     * @param expireTime 过期时间
     */
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 获取是否已使用。
     *
     * @return 是否已使用：1是 0否
     */
    public Integer getUsed() {
        return used;
    }

    /**
     * 设置是否已使用。
     *
     * @param used 是否已使用：1是 0否
     */
    public void setUsed(Integer used) {
        this.used = used;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
