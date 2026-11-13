package com.campus.repair.controller;

import com.campus.repair.common.Result;
import com.campus.repair.common.UserContext;
import com.campus.repair.dto.AiChatSendDTO;
import com.campus.repair.service.AiChatService;
import com.campus.repair.vo.AiChatMessageVO;
import com.campus.repair.vo.AiChatSessionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 智能报修助手接口：会话管理与对话。
 * 全部接口均需登录 token，仅可操作本人会话。
 *
 * @author campus
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    /** AI 对话服务 */
    private final AiChatService aiChatService;

    /**
     * 构造 AI 对话控制器。
     *
     * @param aiChatService AI 对话服务
     */
    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 创建新会话。
     *
     * @param title 会话标题（可选，为空时默认「新会话」）
     * @return 会话详情
     */
    @PostMapping("/sessions")
    public Result<AiChatSessionVO> createSession(
            @RequestParam(value = "title", required = false) String title) {
        return Result.success(aiChatService.createSession(UserContext.getUserId(), title));
    }

    /**
     * 查询当前用户的会话列表（按更新时间倒序）。
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Result<List<AiChatSessionVO>> listSessions() {
        return Result.success(aiChatService.listSessions(UserContext.getUserId()));
    }

    /**
     * 删除会话及其全部消息。
     *
     * @param id 会话ID
     * @return 成功响应
     */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable("id") Long id) {
        aiChatService.deleteSession(UserContext.getUserId(), id);
        return Result.success(null);
    }

    /**
     * 查询某会话的消息列表（按创建时间正序）。
     *
     * @param id 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{id}/messages")
    public Result<List<AiChatMessageVO>> listMessages(@PathVariable("id") Long id) {
        return Result.success(aiChatService.listMessages(UserContext.getUserId(), id));
    }

    /**
     * 发送一条用户消息并返回 AI 回答。
     * 若 sessionId 为空则自动创建新会话（标题取提问摘要）。
     *
     * @param dto 发送消息请求（sessionId 可选，prompt 必填）
     * @return AI 回答消息
     */
    @PostMapping("/messages")
    public Result<AiChatMessageVO> sendMessage(@Valid @RequestBody AiChatSendDTO dto) {
        return Result.success(aiChatService.sendMessage(UserContext.getUserId(), dto));
    }
}
