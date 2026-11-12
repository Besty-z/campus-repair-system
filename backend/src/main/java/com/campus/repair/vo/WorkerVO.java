package com.campus.repair.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 维修工返回对象（管理员列表、派单下拉与维修工本人档案通用）。
 *
 * @author campus
 */
public class WorkerVO implements Serializable {

    /** 维修工档案ID（repair_worker.id） */
    private Long id;

    /** 维修工用户ID（sys_user.id） */
    private Long userId;

    /** 维修工姓名（sys_user.nickname） */
    private String name;

    /** 手机号 */
    private String phone;

    /** 维修工简介 */
    private String intro;

    /** 状态：1在职 0停用 */
    private Integer status;

    /** 擅长故障分类ID列表 */
    private List<Long> categoryIds;

    /** 擅长故障分类名称列表 */
    private List<String> categoryNames;

    /** 已完成工单数 */
    private long completedCount;

    /** 平均评分（保留1位小数，无评分时为 null） */
    private Double avgRating;

    /** 评分次数 */
    private long ratingCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 无参构造器。
     */
    public WorkerVO() {
    }

    /**
     * 获取维修工档案ID。
     *
     * @return 维修工档案ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置维修工档案ID。
     *
     * @param id 维修工档案ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取维修工用户ID。
     *
     * @return 维修工用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置维修工用户ID。
     *
     * @param userId 维修工用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
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
     * @param categoryIds 擅长故障分类ID列表
     */
    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    /**
     * 获取擅长故障分类名称列表。
     *
     * @return 擅长故障分类名称列表
     */
    public List<String> getCategoryNames() {
        return categoryNames;
    }

    /**
     * 设置擅长故障分类名称列表。
     *
     * @param categoryNames 擅长故障分类名称列表
     */
    public void setCategoryNames(List<String> categoryNames) {
        this.categoryNames = categoryNames;
    }

    /**
     * 获取已完成工单数。
     *
     * @return 已完成工单数
     */
    public long getCompletedCount() {
        return completedCount;
    }

    /**
     * 设置已完成工单数。
     *
     * @param completedCount 已完成工单数
     */
    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    /**
     * 获取平均评分。
     *
     * @return 平均评分（保留1位小数），无评分时为 null
     */
    public Double getAvgRating() {
        return avgRating;
    }

    /**
     * 设置平均评分。
     *
     * @param avgRating 平均评分
     */
    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    /**
     * 获取评分次数。
     *
     * @return 评分次数
     */
    public long getRatingCount() {
        return ratingCount;
    }

    /**
     * 设置评分次数。
     *
     * @param ratingCount 评分次数
     */
    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
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
}
