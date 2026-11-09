package com.campus.repair.vo;

import com.campus.repair.entity.SysUser;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户管理分页记录返回对象（仅管理员可见）。
 *
 * @author campus
 */
public class AdminUserVO implements Serializable {

    /** 用户ID */
    private Long id;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 角色 */
    private String role;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createTime;

    /**
     * 无参构造器。
     */
    public AdminUserVO() {
    }

    /**
     * 由用户实体构建返回对象。
     *
     * @param user 用户实体
     * @return 用户管理返回对象
     */
    public static AdminUserVO from(SysUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 获取用户ID。
     *
     * @return 用户ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户ID。
     *
     * @param id 用户ID
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
     * 获取昵称。
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取角色。
     *
     * @return 角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置角色。
     *
     * @param role 角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取状态。
     *
     * @return 状态：1正常 0禁用
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态：1正常 0禁用
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取注册时间。
     *
     * @return 注册时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置注册时间。
     *
     * @param createTime 注册时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
