package com.campus.repair.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 维修工维护本人档案请求对象：简介与擅长分类。
 *
 * @author campus
 */
public class WorkerSelfDTO {

    /** 维修工简介 */
    @Size(max = 200, message = "维修工简介长度不能超过200个字符")
    private String intro;

    /** 擅长故障分类ID列表（可多选，传空表示清空） */
    private List<Long> categoryIds;

    /**
     * 无参构造器。
     */
    public WorkerSelfDTO() {
    }

    /**
     * 获取维修工简介。
     *
     * @return 维修工简介
     */
    public String getIntro() {
        return intro;
    }

    /**
     * 设置维修工简介。
     *
     * @param intro 维修工简介
     */
    public void setIntro(String intro) {
        this.intro = intro;
    }

    /**
     * 获取擅长故障分类ID列表。
     *
     * @return 擅长故障分类ID列表
     */
    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    /**
     * 设置擅长故障分类ID列表。
     *
     * @param categoryIds 擅长故障分类ID列表（可多选）
     */
    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
