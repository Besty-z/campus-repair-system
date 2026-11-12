package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 课程演示用的身份切换请求对象。
 * 同一账号可在普通用户、会员、维修工、管理员之间切换，便于双端演示时免注册多个账号。
 *
 * @author campus
 */
public class RoleSwitchDTO {

    /** 目标角色：USER / VIP / WORKER / ADMIN */
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "USER|VIP|WORKER|ADMIN", message = "角色只能为USER、VIP、WORKER或ADMIN")
    private String role;

    /**
     * 获取目标角色。
     *
     * @return 目标角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置目标角色。
     *
     * @param role 目标角色
     */
    public void setRole(String role) {
        this.role = role;
    }
}
