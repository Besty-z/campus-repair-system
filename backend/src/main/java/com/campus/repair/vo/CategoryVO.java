package com.campus.repair.vo;

import com.campus.repair.entity.FaultCategory;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 故障分类返回对象（用户端，仅启用状态分类）。
 *
 * @author campus
 */
public class CategoryVO implements Serializable {

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 基础服务价（元） */
    private BigDecimal price;

    /** 服务图片URL */
    private String imageUrl;

    /** 排序号 */
    private Integer sort;

    /**
     * 无参构造器。
     */
    public CategoryVO() {
    }

    /**
     * 由分类实体构建返回对象。
     *
     * @param category 分类实体
     * @return 分类返回对象
     */
    public static CategoryVO from(FaultCategory category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setPrice(category.getPrice());
        vo.setImageUrl(category.getImageUrl());
        vo.setSort(category.getSort());
        return vo;
    }

    /**
     * 获取分类ID。
     *
     * @return 分类ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置分类ID。
     *
     * @param id 分类ID
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
}
