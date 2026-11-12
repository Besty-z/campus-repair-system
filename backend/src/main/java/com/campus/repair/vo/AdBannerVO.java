package com.campus.repair.vo;

import com.campus.repair.entity.AdBanner;

import java.io.Serializable;

/**
 * 首页广告位返回对象（仅启用状态广告，按排序号升序）。
 *
 * @author campus
 */
public class AdBannerVO implements Serializable {

    /** 广告ID */
    private Long id;

    /** 广告标题 */
    private String title;

    /** 广告副标题 */
    private String subtitle;

    /** 广告图片URL */
    private String imageUrl;

    /**
     * 无参构造器。
     */
    public AdBannerVO() {
    }

    /**
     * 由广告位实体构建返回对象。
     *
     * @param banner 广告位实体
     * @return 广告位返回对象
     */
    public static AdBannerVO from(AdBanner banner) {
        AdBannerVO vo = new AdBannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setSubtitle(banner.getSubtitle());
        vo.setImageUrl(banner.getImageUrl());
        return vo;
    }

    /**
     * 获取广告ID。
     *
     * @return 广告ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置广告ID。
     *
     * @param id 广告ID
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
}
