package com.campus.repair.vo;

import java.io.Serializable;

/**
 * 登录/注册成功返回对象：令牌 + 用户信息。
 *
 * @author campus
 */
public class LoginVO implements Serializable {

    /** JWT 登录令牌（有效期 7 天） */
    private String token;

    /** 当前登录用户信息 */
    private UserVO user;

    /**
     * 无参构造器。
     */
    public LoginVO() {
    }

    /**
     * 全参构造器。
     *
     * @param token 登录令牌
     * @param user  用户信息
     */
    public LoginVO(String token, UserVO user) {
        this.token = token;
        this.user = user;
    }

    /**
     * 获取登录令牌。
     *
     * @return 登录令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置登录令牌。
     *
     * @param token 登录令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取用户信息。
     *
     * @return 用户信息
     */
    public UserVO getUser() {
        return user;
    }

    /**
     * 设置用户信息。
     *
     * @param user 用户信息
     */
    public void setUser(UserVO user) {
        this.user = user;
    }
}
