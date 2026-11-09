package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 工单故障图片实体，对应表 repair_order_image。
 *
 * @author campus
 */
@TableName("repair_order_image")
public class RepairOrderImage {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long orderId;

    /** 图片访问URL（本地存储相对路径） */
    private String imageUrl;

    /** 图片顺序 */
    private Integer sort;

    /** 上传时间 */
    private LocalDateTime createTime;

    /**
     * 无参构造器。
     */
    public RepairOrderImage() {
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
     * 获取图片访问URL。
     *
     * @return 图片访问URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置图片访问URL。
     *
     * @param imageUrl 图片访问URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取图片顺序。
     *
     * @return 图片顺序
     */
    public Integer getSort() {
        return sort;
    }

    /**
     * 设置图片顺序。
     *
     * @param sort 图片顺序
     */
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    /**
     * 获取上传时间。
     *
     * @return 上传时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置上传时间。
     *
     * @param createTime 上传时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
