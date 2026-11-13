package com.campus.repair.ai;

import java.util.List;

/**
 * 大模型客户端接口层。
 * 业务层只依赖此接口，不关心具体实现；
 * 当请求未携带 API Key 且服务端未配置默认 Key 时，实现类应回退到模拟回答，
 * 保证报修助手在无密钥场景下仍可用。
 *
 * @author campus
 */
public interface LlmClient {

    /**
     * 向大模型发起对话请求，返回 AI 回答文本。
     *
     * @param messages 对话上下文（含 system 提示、历史 user/assistant 消息与当前 user 消息）
     * @param apiKey   用户在前端/小程序配置的 API Key（可选）；为空时使用服务端默认 Key，
     *                 若默认 Key 也为空则回退到模拟回答
     * @return AI 回答文本
     */
    String chat(List<ChatMessage> messages, String apiKey);
}
