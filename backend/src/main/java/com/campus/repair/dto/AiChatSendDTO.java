package com.campus.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 对话发送消息请求对象。
 *
 * @author campus
 */
public class AiChatSendDTO {

    /** 会话ID（为空时自动创建新会话） */
    private Long sessionId;

    /** 用户提问内容 */
    @NotBlank(message = "提问内容不能为空")
    @Size(max = 1000, message = "提问内容长度不能超过1000个字符")
    private String prompt;

    /**
     * 用户在前端/小程序配置的 DeepSeek API Key（可选）。
     * 仅在单次请求内透传给大模型服务，不会被持久化。
     */
    @Size(max = 200, message = "API Key 长度不能超过200个字符")
    private String apiKey;

    /**
     * 无参构造器。
     */
    public AiChatSendDTO() {
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
     * 获取用户提问内容。
     *
     * @return 用户提问内容
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * 设置用户提问内容。
     *
     * @param prompt 用户提问内容
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * 获取用户配置的 API Key。
     *
     * @return API Key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置用户配置的 API Key。
     *
     * @param apiKey API Key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
