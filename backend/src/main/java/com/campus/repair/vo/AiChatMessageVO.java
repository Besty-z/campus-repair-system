package com.campus.repair.vo;

import com.campus.repair.entity.AiChatMessage;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 消息返回对象。
 *
 * @author campus
 */
public class AiChatMessageVO implements Serializable {

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 角色：user用户 assistantAI system系统提示 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 无参构造器。
     */
    public AiChatMessageVO() {
    }

    /**
     * 由消息实体构建返回对象。
     *
     * @param message 消息实体
     * @return 消息返回对象
     */
    public static AiChatMessageVO from(AiChatMessage message) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    /**
     * 获取消息ID。
     *
     * @return 消息ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置消息ID。
     *
     * @param id 消息ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取会话ID。
     *
     * @return 会话ID
     */
    public Long getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID。
     *
     * @param sessionId 会话ID
     */
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取角色。
     *
     * @return 角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置角色。
     *
     * @param role 角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取消息内容。
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容。
     *
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
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
