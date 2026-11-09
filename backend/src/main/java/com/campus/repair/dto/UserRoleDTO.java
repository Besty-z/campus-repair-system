package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 设置用户身份请求对象（仅支持 USER / VIP，管理员角色不允许通过此接口变更）。
 *
 * @author campus
 */
public class UserRoleDTO {

    /** 目标角色：USER普通用户 VIP会员用户 */
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "USER|VIP", message = "角色只能设置为USER或VIP")
    private String role;

    /**
     * 无参构造器。
     */
    public UserRoleDTO() {
    }

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
