package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 故障分类实体，对应表 fault_category。
 * 分类按维修工种划分，不区分报修地点，由管理员动态维护。
 *
 * @author campus
 */
@TableName("fault_category")
public class FaultCategory {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 基础服务价（元） */
    private BigDecimal price;

    /** 服务图片URL */
    private String imageUrl;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 无参构造器。
     */
    public FaultCategory() {
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
     * 获取分类名称。
     *
     * @return 分类名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置分类名称。
     *
     * @param name 分类名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取基础服务价。
     *
     * @return 基础服务价（元）
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 设置基础服务价。
     *
     * @param price 基础服务价（元）
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * 获取服务图片URL。
     *
     * @return 服务图片URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置服务图片URL。
     *
     * @param imageUrl 服务图片URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取排序号。
     *
     * @return 排序号
     */
    public Integer getSort() {
        return sort;
    }

    /**
     * 设置排序号。
     *
     * @param sort 排序号
     */
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    /**
     * 获取状态。
     *
     * @return 状态：1启用 0停用
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态：1启用 0停用
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
