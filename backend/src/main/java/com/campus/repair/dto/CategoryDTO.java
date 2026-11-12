package com.campus.repair.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 故障分类保存请求对象（新增与编辑共用；status 仅编辑时使用）。
 *
 * @author campus
 */
public class CategoryDTO {

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;

    /** 基础服务价（元） */
    @NotNull(message = "基础服务价不能为空")
    @DecimalMin(value = "0", message = "基础服务价不能为负数")
    private BigDecimal price;

    /** 服务图片URL（可空） */
    @Size(max = 255, message = "服务图片URL长度不能超过255个字符")
    private String imageUrl;

    /** 排序号，越小越靠前 */
    @NotNull(message = "排序号不能为空")
    private Integer sort;

    /** 状态：1启用 0停用（仅编辑接口使用，新增时忽略） */
    private Integer status;

    /**
     * 无参构造器。
     */
    public CategoryDTO() {
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
}
