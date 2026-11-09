package com.campus.repair.vo;

import com.campus.repair.entity.SysUser;

import java.io.Serializable;

/**
 * 登录用户信息返回对象（登录、注册、获取个人信息通用）。
 *
 * @author campus
 */
public class UserVO implements Serializable {

    /** 用户ID */
    private Long id;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 角色 */
    private String role;

    /**
     * 无参构造器。
     */
    public UserVO() {
    }

    /**
     * 由用户实体构建返回对象。
     *
     * @param user 用户实体
     * @return 用户信息返回对象
     */
    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
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
}
