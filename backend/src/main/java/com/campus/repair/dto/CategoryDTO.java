package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    /** 归属地点类型：DORM宿舍楼内 PUBLIC校园公共区域 */
    @NotBlank(message = "地点类型不能为空")
    @Pattern(regexp = "DORM|PUBLIC", message = "地点类型只能为DORM或PUBLIC")
    private String locationType;

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
