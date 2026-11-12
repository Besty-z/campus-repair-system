package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 维修工档案实体，对应表 repair_worker。
 * 每个维修工对应一个角色为 WORKER 的系统用户，档案记录简介与在职状态。
 *
 * @author campus
 */
@TableName("repair_worker")
public class RepairWorker {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 维修工用户ID（关联 sys_user.id） */
    private Long userId;

    /** 维修工简介 */
    private String intro;

    /** 状态：1在职 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 无参构造器。
     */
    public RepairWorker() {
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
