package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 首页广告位实体，对应表 ad_banner。
 * 用户端首页按排序号升序展示启用状态的广告。
 *
 * @author campus
 */
@TableName("ad_banner")
public class AdBanner {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 广告标题 */
    private String title;

    /** 广告副标题 */
    private String subtitle;

    /** 广告图片URL */
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
    public AdBanner() {
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
     * 获取广告标题。
     *
     * @return 广告标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置广告标题。
     *
     * @param title 广告标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取广告副标题。
     *
     * @return 广告副标题
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * 设置广告副标题。
     *
     * @param subtitle 广告副标题
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * 获取广告图片URL。
     *
     * @return 广告图片URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置广告图片URL。
     *
     * @param imageUrl 广告图片URL
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
