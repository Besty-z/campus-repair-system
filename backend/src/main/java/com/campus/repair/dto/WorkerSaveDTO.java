package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 管理员新增/编辑维修工请求对象。
 * 手机号仅在新增时必填（服务层校验）；编辑时以档案ID为准，忽略手机号。
 *
 * @author campus
 */
public class WorkerSaveDTO {

    /** 手机号（新增维修工时必填，已存在则升级为维修工） */
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    /** 维修工姓名（保存在 sys_user.nickname） */
    @NotBlank(message = "维修工姓名不能为空")
    @Size(max = 30, message = "维修工姓名长度不能超过30个字符")
    private String name;

    /** 维修工简介 */
    @Size(max = 200, message = "维修工简介长度不能超过200个字符")
    private String intro;

    /** 擅长故障分类ID列表（可多选） */
    private List<Long> categoryIds;

    /** 状态：1在职 0停用（仅编辑时使用，新增默认在职） */
    private Integer status;

    /**
     * 无参构造器。
     */
    public WorkerSaveDTO() {
    }

    /**
     * 获取手机号。
     *
     * @return 手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置手机号。
     *
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取维修工姓名。
     *
     * @return 维修工姓名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置维修工姓名。
     *
     * @param name 维修工姓名
     */
    public void setName(String name) {
        this.name = name;
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

    /**
     * 获取状态。
     *
     * @return 状态：1在职 0停用
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态：1在职 0停用
     */
    public void setStatus(Integer status) {
        this.status = status;
    }
}
