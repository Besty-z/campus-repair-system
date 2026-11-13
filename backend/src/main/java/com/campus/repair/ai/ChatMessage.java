package com.campus.repair.ai;

/**
 * 大模型对话消息（角色 + 内容）。
 * 用于在 {@link LlmClient} 与业务层之间传递对话上下文。
 *
 * @author campus
 */
public final class ChatMessage {

    /** 角色：system 系统提示 */
    public static final String ROLE_SYSTEM = "system";

    /** 角色：user 用户提问 */
    public static final String ROLE_USER = "user";

    /** 角色：assistant AI 回答 */
    public static final String ROLE_ASSISTANT = "assistant";

    /** 角色 */
    private final String role;

    /** 内容 */
    private final String content;

    /**
     * 构造对话消息。
     *
     * @param role    角色（system/user/assistant）
     * @param content 消息内容
     */
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
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
     * 获取内容。
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }
}
