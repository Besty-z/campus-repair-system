package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AI 智能报修助手消息实体，对应表 ai_chat_message。
 * 按会话保存用户提问（user）与 AI 回答（assistant）。
 *
 * @author campus
 */
@TableName("ai_chat_message")
public class AiChatMessage {

    /** 消息角色：用户提问 */
    public static final String ROLE_USER = "user";

    /** 消息角色：AI 回答 */
    public static final String ROLE_ASSISTANT = "assistant";

    /** 消息角色：系统提示 */
    public static final String ROLE_SYSTEM = "system";

    /** 主键ID */
    @TableId(type = IdType.AUTO)
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
    public AiChatMessage() {
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
