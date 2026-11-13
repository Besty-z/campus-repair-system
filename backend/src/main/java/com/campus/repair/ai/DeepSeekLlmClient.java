package com.campus.repair.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeepSeek 大模型客户端实现（兼容 OpenAI Chat Completions 协议）。
 * <p>
 * 作为唯一的 {@link LlmClient} 实现，支持两种密钥来源：
 * <ol>
 *     <li>请求体中携带的用户 API Key（前端/小程序配置，优先级最高）；</li>
 *     <li>服务端环境变量 {@code DEEPSEEK_API_KEY} 注入的默认 Key。</li>
 * </ol>
 * 当两者均为空时，回退到 {@link MockLlmClient} 的模拟报修建议，保证助手在无密钥场景下仍可用。
 * API Key 仅在单次请求内透传给 DeepSeek，不会被持久化。
 *
 * @author campus
 */
@Component
public class DeepSeekLlmClient implements LlmClient {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmClient.class);

    /** 对话补全接口路径 */
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /** HTTP 客户端 */
    private final RestTemplate restTemplate;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** 模拟回答回退组件 */
    private final MockLlmClient mockLlmClient;

    /** DeepSeek API 基础地址 */
    private final String baseUrl;

    /** API Key（从环境变量注入，作为服务端默认 Key） */
    private final String apiKey;

    /** 模型名称 */
    private final String model;

    /**
     * 构造 DeepSeek 客户端。
     *
     * @param restTemplate HTTP 客户端
     * @param objectMapper JSON 序列化工具
     * @param mockLlmClient 模拟回答回退组件
     * @param baseUrl      API 基础地址
     * @param apiKey       服务端默认 API Key（环境变量注入）
     * @param model        模型名称
     */
    public DeepSeekLlmClient(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             MockLlmClient mockLlmClient,
                             @Value("${llm.base-url:https://api.deepseek.com}") String baseUrl,
                             @Value("${llm.api-key:}") String apiKey,
                             @Value("${llm.model:deepseek-chat}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.mockLlmClient = mockLlmClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 调用 DeepSeek Chat Completions 接口，返回 AI 回答文本。
     * <p>
     * 密钥选择优先级：请求体 apiKey {@literal >} 服务端默认 apiKey；均为空时回退到模拟回答。
     *
     * @param messages 对话上下文
     * @param apiKey   请求携带的用户 API Key（可为空）
     * @return AI 回答文本
     * @throws RuntimeException 接口调用失败或响应结构异常时抛出
     */
    @Override
    public String chat(List<ChatMessage> messages, String apiKey) {
        String effectiveKey = StringUtils.hasText(apiKey) ? apiKey.trim() : this.apiKey;
        if (!StringUtils.hasText(effectiveKey)) {
            log.info("[DeepSeek] 未提供 API Key，回退到模拟回答");
            return mockLlmClient.mockAnswer(messages);
        }

        String url = baseUrl + CHAT_COMPLETIONS_PATH;

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.7);
        requestBody.put("messages", messages.stream()
                .map(m -> {
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", m.getRole());
                    msg.put("content", m.getContent());
                    return msg;
                })
                .collect(Collectors.toList()));

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(effectiveKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("[DeepSeek] 调用对话接口，消息数：{}，使用{}密钥",
                messages.size(), StringUtils.hasText(apiKey) ? "请求级" : "服务端默认");

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                log.error("[DeepSeek] 响应结构异常：{}", response);
                throw new RuntimeException("DeepSeek 响应结构异常，未找到回答内容");
            }
            String answer = content.asText();
            log.info("[DeepSeek] 回答长度：{}", answer.length());
            return answer;
        } catch (Exception ex) {
            log.error("[DeepSeek] 调用失败", ex);
            throw new RuntimeException("DeepSeek 调用失败：" + ex.getMessage(), ex);
        }
    }
}
