package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 维修工-擅长分类关联实体，对应表 repair_worker_category。
 * 一个维修工可擅长多个故障分类（多选）。
 *
 * @author campus
 */
@TableName("repair_worker_category")
public class RepairWorkerCategory {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 维修工档案ID */
    private Long workerId;

    /** 故障分类ID */
    private Long categoryId;

    /**
     * 无参构造器。
     */
    public RepairWorkerCategory() {
    }

    /**
     * 构造维修工-分类关联。
     *
     * @param workerId   维修工档案ID
     * @param categoryId 故障分类ID
     */
    public RepairWorkerCategory(Long workerId, Long categoryId) {
        this.workerId = workerId;
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
     * 获取维修工档案ID。
     *
     * @return 维修工档案ID
     */
    public Long getWorkerId() {
        return workerId;
    }

    /**
     * 设置维修工档案ID。
     *
     * @param workerId 维修工档案ID
     */
    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
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
