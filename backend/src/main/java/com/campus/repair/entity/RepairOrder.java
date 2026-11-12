package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报修工单实体，对应表 repair_order。
 * 状态流转：PENDING → IN_PROGRESS → FINISHED；PENDING/IN_PROGRESS → REJECTED。
 *
 * @author campus
 */
@TableName("repair_order")
public class RepairOrder {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号（R+日期+序号） */
    private String orderNo;

    /** 报修人ID */
    private Long userId;

    /** 地点类型：DORM宿舍楼内 PUBLIC校园公共区域 */
    private String locationType;

    /** 宿舍楼号（DORM 时填写） */
    private String buildingNo;

    /** 宿舍房间号（DORM 时填写） */
    private String roomNo;

    /** 公共区域具体位置（PUBLIC 时填写） */
    private String publicLocation;

    /** 故障分类ID（选择其他自定义类型时为 null） */
    private Long categoryId;

    /** 自定义故障类型（用户选择“其他”时填写） */
    private String customCategory;

    /** 预估费用（元，会员已按8折计算；选择“其他”时为 null 表示面议） */
    private BigDecimal estimatedPrice;

    /** 故障文字描述 */
    private String description;

    /** 是否紧急：1紧急 0普通（仅会员可标记） */
    private Integer urgent;

    /** 会员自选预约上门时间（普通用户为 NULL） */
    private LocalDateTime appointmentTime;

    /** 工单状态：PENDING待接单 IN_PROGRESS维修中 FINISHED已完成 REJECTED驳回 */
    private String status;

    /** 维修备注（管理员填写） */
    private String adminRemark;

    /** 接单管理员ID */
    private Long adminId;

    /** 维修工用户ID（关联 sys_user.id） */
    private Long workerId;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 评分（1-5星，完成后由报修人评价） */
    private Integer ratingScore;

    /** 评价内容 */
    private String ratingComment;

    /** 评价时间 */
    private LocalDateTime ratingTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 无参构造器。
     */
    public RepairOrder() {
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
     * 获取工单编号。
     *
     * @return 工单编号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置工单编号。
     *
     * @param orderNo 工单编号
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取报修人ID。
     *
     * @return 报修人ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置报修人ID。
     *
     * @param userId 报修人ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取地点类型。
     *
     * @return 地点类型
     */
    public String getLocationType() {
        return locationType;
    }

    /**
     * 设置地点类型。
     *
     * @param locationType 地点类型
     */
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    /**
     * 获取宿舍楼号。
     *
     * @return 宿舍楼号
     */
    public String getBuildingNo() {
        return buildingNo;
    }

    /**
     * 设置宿舍楼号。
     *
     * @param buildingNo 宿舍楼号
     */
    public void setBuildingNo(String buildingNo) {
        this.buildingNo = buildingNo;
    }

    /**
     * 获取宿舍房间号。
     *
     * @return 宿舍房间号
     */
    public String getRoomNo() {
        return roomNo;
    }

    /**
     * 设置宿舍房间号。
     *
     * @param roomNo 宿舍房间号
     */
    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    /**
     * 获取公共区域具体位置。
     *
     * @return 公共区域具体位置
     */
    public String getPublicLocation() {
        return publicLocation;
    }

    /**
     * 设置公共区域具体位置。
     *
     * @param publicLocation 公共区域具体位置
     */
    public void setPublicLocation(String publicLocation) {
        this.publicLocation = publicLocation;
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

    /**
     * 获取自定义故障类型。
     *
     * @return 自定义故障类型（用户选择“其他”时填写）
     */
    public String getCustomCategory() {
        return customCategory;
    }

    /**
     * 设置自定义故障类型。
     *
     * @param customCategory 自定义故障类型（用户选择“其他”时填写）
     */
    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    /**
     * 获取预估费用。
     *
     * @return 预估费用（元），选择“其他”时为 null
     */
    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    /**
     * 设置预估费用。
     *
     * @param estimatedPrice 预估费用（元）
     */
    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    /**
     * 获取故障文字描述。
     *
     * @return 故障文字描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置故障文字描述。
     *
     * @param description 故障文字描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取是否紧急。
     *
     * @return 是否紧急：1紧急 0普通
     */
    public Integer getUrgent() {
        return urgent;
    }

    /**
     * 设置是否紧急。
     *
     * @param urgent 是否紧急：1紧急 0普通
     */
    public void setUrgent(Integer urgent) {
        this.urgent = urgent;
    }

    /**
     * 获取预约上门时间。
     *
     * @return 预约上门时间
     */
    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    /**
     * 设置预约上门时间。
     *
     * @param appointmentTime 预约上门时间
     */
    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    /**
     * 获取工单状态。
     *
     * @return 工单状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置工单状态。
     *
     * @param status 工单状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取维修备注。
     *
     * @return 维修备注
     */
    public String getAdminRemark() {
        return adminRemark;
    }

    /**
     * 设置维修备注。
     *
     * @param adminRemark 维修备注
     */
    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
    }

    /**
     * 获取接单管理员ID。
     *
     * @return 接单管理员ID
     */
    public Long getAdminId() {
        return adminId;
    }

    /**
     * 设置接单管理员ID。
     *
     * @param adminId 接单管理员ID
     */
    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    /**
     * 获取维修工用户ID。
     *
     * @return 维修工用户ID
     */
    public Long getWorkerId() {
        return workerId;
    }

    /**
     * 设置维修工用户ID。
     *
     * @param workerId 维修工用户ID
     */
    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    /**
     * 获取接单时间。
     *
     * @return 接单时间
     */
    public LocalDateTime getAcceptTime() {
        return acceptTime;
    }

    /**
     * 设置接单时间。
     *
     * @param acceptTime 接单时间
     */
    public void setAcceptTime(LocalDateTime acceptTime) {
        this.acceptTime = acceptTime;
    }

    /**
     * 获取完成时间。
     *
     * @return 完成时间
     */
    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    /**
     * 设置完成时间。
     *
     * @param finishTime 完成时间
     */
    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * 获取评分。
     *
     * @return 评分（1-5星），未评价时为 null
     */
    public Integer getRatingScore() {
        return ratingScore;
    }

    /**
     * 设置评分。
     *
     * @param ratingScore 评分（1-5星）
     */
    public void setRatingScore(Integer ratingScore) {
        this.ratingScore = ratingScore;
    }

    /**
     * 获取评价内容。
     *
     * @return 评价内容
     */
    public String getRatingComment() {
        return ratingComment;
    }

    /**
     * 设置评价内容。
     *
     * @param ratingComment 评价内容
     */
    public void setRatingComment(String ratingComment) {
        this.ratingComment = ratingComment;
    }

    /**
     * 获取评价时间。
     *
     * @return 评价时间
     */
    public LocalDateTime getRatingTime() {
        return ratingTime;
    }

    /**
     * 设置评价时间。
     *
     * @param ratingTime 评价时间
     */
    public void setRatingTime(LocalDateTime ratingTime) {
        this.ratingTime = ratingTime;
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
