package com.campus.repair.vo;

import com.campus.repair.entity.AiChatSession;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话返回对象。
 *
 * @author campus
 */
public class AiChatSessionVO implements Serializable {

    /** 会话ID */
    private Long id;

    /** 会话标题 */
    private String title;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 无参构造器。
     */
    public AiChatSessionVO() {
    }

    /**
     * 由会话实体构建返回对象。
     *
     * @param session 会话实体
     * @return 会话返回对象
     */
    public static AiChatSessionVO from(AiChatSession session) {
        AiChatSessionVO vo = new AiChatSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    /**
     * 获取会话ID。
     *
     * @return 会话ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置会话ID。
     *
     * @param id 会话ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取会话标题。
     *
     * @return 会话标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置会话标题。
     *
     * @param title 会话标题
     */
    public void setTitle(String title) {
        this.title = title;
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
