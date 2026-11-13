package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AI 智能报修助手会话实体，对应表 ai_chat_session。
 * 一个用户可拥有多个会话，按更新时间倒序展示。
 *
 * @author campus
 */
@TableName("ai_chat_session")
public class AiChatSession {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 会话标题（取首条用户消息摘要） */
    private String title;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 无参构造器。
     */
    public AiChatSession() {
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
     * 获取所属用户ID。
     *
     * @return 所属用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置所属用户ID。
     *
     * @param userId 所属用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
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
