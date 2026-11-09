package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员修改工单状态请求对象。
 *
 * @author campus
 */
public class OrderStatusDTO {

    /** 目标状态：IN_PROGRESS维修中 FINISHED已完成 REJECTED驳回 */
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "IN_PROGRESS|FINISHED|REJECTED", message = "目标状态只能为IN_PROGRESS、FINISHED或REJECTED")
    private String status;

    /** 维修备注（选填） */
    @Size(max = 500, message = "维修备注长度不能超过500个字符")
    private String adminRemark;

    /**
     * 无参构造器。
     */
    public OrderStatusDTO() {
    }

    /**
     * 获取目标状态。
     *
     * @return 目标状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置目标状态。
     *
     * @param status 目标状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取维修备注。
     *
     * @return 维修备注
     */
    public String getAdminRemark() {
        return adminRemark;
    }

    /**
     * 设置维修备注。
     *
     * @param adminRemark 维修备注
     */
    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
    }
}
