package com.campus.repair.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员派单请求对象：把工单指派给指定维修工。
 *
 * @author campus
 */
public class OrderAssignDTO {

    /** 维修工用户ID（关联 sys_user.id） */
    @NotNull(message = "维修工不能为空")
    private Long workerId;

    /**
     * 无参构造器。
     */
    public OrderAssignDTO() {
    }

    /**
     * 获取维修工用户ID。
     *
     * @return 维修工用户ID
     */
    public Long getWorkerId() {
        return workerId;
    }

    /**
     * 设置维修工用户ID。
     *
     * @param workerId 维修工用户ID
     */
    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }
}
