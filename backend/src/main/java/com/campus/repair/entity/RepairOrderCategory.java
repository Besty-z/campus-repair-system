package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 工单-故障分类关联实体，对应表 repair_order_category。
 * 一个报修工单可选择多个故障分类（多选）。
 *
 * @author campus
 */
@TableName("repair_order_category")
public class RepairOrderCategory {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long orderId;

    /** 故障分类ID */
    private Long categoryId;

    /**
     * 无参构造器。
     */
    public RepairOrderCategory() {
    }

    /**
     * 构造工单-分类关联。
     *
     * @param orderId    工单ID
     * @param categoryId 故障分类ID
     */
    public RepairOrderCategory(Long orderId, Long categoryId) {
        this.orderId = orderId;
        this.categoryId = categoryId;
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
     * 获取工单ID。
     *
     * @return 工单ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 设置工单ID。
     *
     * @param orderId 工单ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 获取故障分类ID。
     *
     * @return 故障分类ID
     */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * 设置故障分类ID。
     *
     * @param categoryId 故障分类ID
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
