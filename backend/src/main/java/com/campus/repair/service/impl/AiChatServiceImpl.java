package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.ai.ChatMessage;
import com.campus.repair.ai.LlmClient;
import com.campus.repair.common.BizException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.dto.AiChatSendDTO;
import com.campus.repair.entity.AiChatMessage;
import com.campus.repair.entity.AiChatSession;
import com.campus.repair.mapper.AiChatMessageMapper;
import com.campus.repair.mapper.AiChatSessionMapper;
import com.campus.repair.service.AiChatService;
import com.campus.repair.vo.AiChatMessageVO;
import com.campus.repair.vo.AiChatSessionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 智能报修助手服务实现。
 * 负责会话与消息的持久化，以及调用 {@link LlmClient} 获取 AI 回答。
 *
 * @author campus
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    /** 日志对象 */
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 默认会话标题 */
    private static final String DEFAULT_SESSION_TITLE = "新会话";

    /** 会话标题最大长度 */
    private static final int TITLE_MAX_LEN = 20;

    /** 对话上下文最大消息数（含 system 提示），超出则只保留最近 N-1 条 */
    private static final int HISTORY_MAX_SIZE = 20;

    /** 系统提示词：约束 AI 扮演校园报修智能助手 */
    private static final String SYSTEM_PROMPT =
            "你是校园报修系统的智能助手，帮助同学描述故障、判断故障分类、"
                    + "给出自助排查建议，并在需要时引导提交报修工单。回答使用中文，"
                    + "结构清晰、简洁实用，不编造事实。";

    /** 会话 Mapper */
    private final AiChatSessionMapper sessionMapper;

    /** 消息 Mapper */
    private final AiChatMessageMapper messageMapper;

    /** 大模型客户端 */
    private final LlmClient llmClient;

    /**
     * 构造 AI 对话服务。
     *
     * @param sessionMapper 会话 Mapper
     * @param messageMapper 消息 Mapper
     * @param llmClient     大模型客户端
     */
    public AiChatServiceImpl(AiChatSessionMapper sessionMapper,
                             AiChatMessageMapper messageMapper,
                             LlmClient llmClient) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.llmClient = llmClient;
    }

    /**
     * 创建新会话。
     *
     * @param userId 当前登录用户ID
     * @param title  会话标题（为空时使用默认「新会话」）
     * @return 会话返回对象
     */
    @Override
    public AiChatSessionVO createSession(Long userId, String title) {
        LocalDateTime now = LocalDateTime.now();
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(title) ? title : DEFAULT_SESSION_TITLE);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        log.info("用户 {} 创建 AI 会话 {}，标题：{}", userId, session.getId(), session.getTitle());
        return AiChatSessionVO.from(session);
    }

    /**
     * 查询当前用户的会话列表（按更新时间倒序）。
     *
     * @param userId 当前登录用户ID
     * @return 会话列表
     */
    @Override
    public List<AiChatSessionVO> listSessions(Long userId) {
        List<AiChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .orderByDesc(AiChatSession::getUpdateTime));
        return sessions.stream().map(AiChatSessionVO::from).collect(Collectors.toList());
    }

    /**
     * 删除会话及其全部消息（仅会话所属用户可删除）。
     *
     * @param userId    当前登录用户ID
     * @param sessionId 会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, Long sessionId) {
        AiChatSession session = getOwnedSession(userId, sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, session.getId()));
        sessionMapper.deleteById(session.getId());
        log.info("用户 {} 删除 AI 会话 {}", userId, sessionId);
    }

    /**
     * 查询某会话的消息列表（按创建时间正序）。
     *
     * @param userId    当前登录用户ID
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Override
    public List<AiChatMessageVO> listMessages(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByAsc(AiChatMessage::getCreateTime));
        return messages.stream().map(AiChatMessageVO::from).collect(Collectors.toList());
    }

    /**
     * 发送一条用户消息并返回 AI 回答。
     *
     * @param userId 当前登录用户ID
     * @param dto    发送消息请求
     * @return AI 回答消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatMessageVO sendMessage(Long userId, AiChatSendDTO dto) {
        Long sessionId = dto.getSessionId();
        AiChatSession session;
        LocalDateTime now = LocalDateTime.now();

        // 1. 确定会话：传入 sessionId 则校验归属，否则新建会话
        if (sessionId != null) {
            session = getOwnedSession(userId, sessionId);
        } else {
            session = new AiChatSession();
            session.setUserId(userId);
            session.setTitle(buildTitle(dto.getPrompt()));
            session.setCreateTime(now);
            session.setUpdateTime(now);
            sessionMapper.insert(session);
            sessionId = session.getId();
            log.info("用户 {} 发送消息时自动创建 AI 会话 {}", userId, sessionId);
        }

        // 2. 保存用户消息
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole(AiChatMessage.ROLE_USER);
        userMsg.setContent(dto.getPrompt());
        userMsg.setCreateTime(now);
        messageMapper.insert(userMsg);

        // 3. 构建对话上下文（system + 历史 + 当前用户消息）并调用大模型
        List<ChatMessage> context = buildContext(sessionId, dto.getPrompt());
        String answer;
        try {
            answer = llmClient.chat(context, dto.getApiKey());
        } catch (RuntimeException ex) {
            log.error("调用大模型失败，用户 {} 会话 {}", userId, sessionId, ex);
            answer = "抱歉，AI 助手暂时不可用，请稍后重试或直接提交报修工单。";
        }

        // 4. 保存 AI 回答
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole(AiChatMessage.ROLE_ASSISTANT);
        aiMsg.setContent(answer);
        aiMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(aiMsg);

        // 5. 更新会话更新时间
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        return AiChatMessageVO.from(aiMsg);
    }

    /**
     * 构建发送给大模型的对话上下文：系统提示 + 最近历史消息 + 当前用户消息。
     * 为控制 token 消耗，历史消息最多保留 HISTORY_MAX_SIZE-1 条（不含 system）。
     *
     * @param sessionId 会话ID
     * @param prompt    当前用户提问
     * @return 对话上下文列表
     */
    private List<ChatMessage> buildContext(Long sessionId, String prompt) {
        List<ChatMessage> context = new ArrayList<>();
        context.add(new ChatMessage(ChatMessage.ROLE_SYSTEM, SYSTEM_PROMPT));

        // 取最近历史消息（不含当前刚插入的用户消息，避免重复），按时间正序
        List<AiChatMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .ne(AiChatMessage::getRole, AiChatMessage.ROLE_USER)
                        .or(wrapper -> wrapper
                                .eq(AiChatMessage::getSessionId, sessionId)
                                .eq(AiChatMessage::getRole, AiChatMessage.ROLE_USER)
                                .ne(AiChatMessage::getContent, prompt))
                        .orderByAsc(AiChatMessage::getCreateTime));

        // 历史超过上限时，只保留最近的 HISTORY_MAX_SIZE-1 条
        int start = Math.max(0, history.size() - (HISTORY_MAX_SIZE - 1));
        for (int i = start; i < history.size(); i++) {
            AiChatMessage m = history.get(i);
            context.add(new ChatMessage(m.getRole(), m.getContent()));
        }

        // 追加当前用户消息
        context.add(new ChatMessage(ChatMessage.ROLE_USER, prompt));
        return context;
    }

    /**
     * 校验会话归属并返回会话实体，非本人会话抛出 403。
     *
     * @param userId    当前登录用户ID
     * @param sessionId 会话ID
     * @return 会话实体
     * @throws BizException 会话不存在或非本人
     */
    private AiChatSession getOwnedSession(Long userId, Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }

    /**
     * 根据用户提问生成会话标题（截取前 TITLE_MAX_LEN 个字符）。
     *
     * @param prompt 用户提问
     * @return 会话标题
     */
    private String buildTitle(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return DEFAULT_SESSION_TITLE;
        }
        return prompt.length() > TITLE_MAX_LEN
                ? prompt.substring(0, TITLE_MAX_LEN) + "…"
                : prompt;
    }
}
