package com.campus.repair.service;

import com.campus.repair.dto.AiChatSendDTO;
import com.campus.repair.vo.AiChatMessageVO;
import com.campus.repair.vo.AiChatSessionVO;

import java.util.List;

/**
 * AI 智能报修助手服务：会话管理与对话。
 *
 * @author campus
 */
public interface AiChatService {

    /**
     * 创建新会话。
     *
     * @param userId 当前登录用户ID
     * @param title  会话标题（为空时使用默认「新会话」）
     * @return 会话返回对象
     */
    AiChatSessionVO createSession(Long userId, String title);

    /**
     * 查询当前用户的会话列表（按更新时间倒序）。
     *
     * @param userId 当前登录用户ID
     * @return 会话列表
     */
    List<AiChatSessionVO> listSessions(Long userId);

    /**
     * 删除会话及其全部消息（仅会话所属用户可删除）。
     *
     * @param userId    当前登录用户ID
     * @param sessionId 会话ID
     */
    void deleteSession(Long userId, Long sessionId);

    /**
     * 查询某会话的消息列表（按创建时间正序）。
     *
     * @param userId    当前登录用户ID
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<AiChatMessageVO> listMessages(Long userId, Long sessionId);

    /**
     * 发送一条用户消息并返回 AI 回答。
     * 若 sessionId 为空则自动创建新会话（标题取提问摘要）。
     *
     * @param userId 当前登录用户ID
     * @param dto    发送消息请求（含会话ID与提问内容）
     * @return AI 回答消息
     */
    AiChatMessageVO sendMessage(Long userId, AiChatSendDTO dto);
}
