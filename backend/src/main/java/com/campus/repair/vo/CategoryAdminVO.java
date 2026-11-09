package com.campus.repair.vo;

import com.campus.repair.entity.FaultCategory;

import java.io.Serializable;

/**
 * 故障分类返回对象（管理端，含状态字段，含停用分类）。
 *
 * @author campus
 */
public class CategoryAdminVO implements Serializable {

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 归属地点类型 */
    private String locationType;

    /** 排序号 */
    private Integer sort;

    /** 状态：1启用 0停用 */
    private Integer status;

    /**
     * 无参构造器。
     */
    public CategoryAdminVO() {
    }

    /**
     * 由分类实体构建返回对象。
     *
     * @param category 分类实体
     * @return 管理端分类返回对象
     */
    public static CategoryAdminVO from(FaultCategory category) {
        CategoryAdminVO vo = new CategoryAdminVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setLocationType(category.getLocationType());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
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
     * 获取归属地点类型。
     *
     * @return 地点类型
     */
    public String getLocationType() {
        return locationType;
    }

    /**
     * 设置归属地点类型。
     *
     * @param locationType 地点类型
     */
    public void setLocationType(String locationType) {
        this.locationType = locationType;
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
}
