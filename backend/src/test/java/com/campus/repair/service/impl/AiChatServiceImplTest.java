package com.campus.repair.service.impl;

import com.campus.repair.ai.ChatMessage;
import com.campus.repair.ai.LlmClient;
import com.campus.repair.common.BizException;
import com.campus.repair.common.ResultCode;
import com.campus.repair.dto.AiChatSendDTO;
import com.campus.repair.entity.AiChatMessage;
import com.campus.repair.entity.AiChatSession;
import com.campus.repair.mapper.AiChatMessageMapper;
import com.campus.repair.mapper.AiChatSessionMapper;
import com.campus.repair.vo.AiChatMessageVO;
import com.campus.repair.vo.AiChatSessionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 对话服务单元测试：覆盖会话创建、列表、删除归属校验、发送消息与异常降级。
 *
 * @author campus
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiChatSessionMapper sessionMapper;

    @Mock
    private AiChatMessageMapper messageMapper;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    /**
     * 创建会话应入库并返回 VO。
     */
    @Test
    void createSession_persistsAndReturnsVo() {
        AiChatSessionVO vo = aiChatService.createSession(1L, "测试会话");

        assertEquals("测试会话", vo.getTitle());
        ArgumentCaptor<AiChatSession> captor = ArgumentCaptor.forClass(AiChatSession.class);
        verify(sessionMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("测试会话", captor.getValue().getTitle());
    }

    /**
     * 创建会话标题为空时使用默认「新会话」。
     */
    @Test
    void createSession_emptyTitle_usesDefault() {
        AiChatSessionVO vo = aiChatService.createSession(1L, null);

        assertEquals("新会话", vo.getTitle());
    }

    /**
     * 删除非本人会话应抛出 403。
     */
    @Test
    void deleteSession_notOwner_throwsForbidden() {
        AiChatSession session = new AiChatSession();
        session.setId(10L);
        session.setUserId(2L);
        when(sessionMapper.selectById(10L)).thenReturn(session);

        BizException ex = assertThrows(BizException.class,
                () -> aiChatService.deleteSession(1L, 10L));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    /**
     * 删除不存在的会话应抛出 400。
     */
    @Test
    void deleteSession_notFound_throwsBadRequest() {
        when(sessionMapper.selectById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> aiChatService.deleteSession(1L, 99L));
        assertEquals(ResultCode.BAD_REQUEST, ex.getCode());
    }

    /**
     * 发送消息且 sessionId 为空时，应自动创建新会话并保存用户消息与 AI 回答。
     */
    @Test
    void sendMessage_withoutSessionId_createsSessionAndSavesMessages() {
        when(llmClient.chat(anyList(), any())).thenReturn("AI 回答内容");

        AiChatSendDTO dto = new AiChatSendDTO();
        dto.setPrompt("宿舍水龙头漏水");

        AiChatMessageVO vo = aiChatService.sendMessage(1L, dto);

        assertEquals(AiChatMessage.ROLE_ASSISTANT, vo.getRole());
        assertEquals("AI 回答内容", vo.getContent());
        // 自动创建了会话
        verify(sessionMapper).insert(any(AiChatSession.class));
        // 保存了用户消息与 AI 回答两条消息
        ArgumentCaptor<AiChatMessage> msgCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(msgCaptor.capture());
        List<AiChatMessage> saved = msgCaptor.getAllValues();
        assertEquals(AiChatMessage.ROLE_USER, saved.get(0).getRole());
        assertEquals("宿舍水龙头漏水", saved.get(0).getContent());
        assertEquals(AiChatMessage.ROLE_ASSISTANT, saved.get(1).getRole());
        assertEquals("AI 回答内容", saved.get(1).getContent());
    }

    /**
     * 发送消息时大模型调用异常，应降级返回友好提示并仍保存 AI 消息。
     */
    @Test
    void sendMessage_llmThrows_fallbackAnswer() {
        when(llmClient.chat(anyList(), any())).thenThrow(new RuntimeException("LLM 超时"));

        AiChatSession session = new AiChatSession();
        session.setId(5L);
        session.setUserId(1L);
        when(sessionMapper.selectById(5L)).thenReturn(session);

        AiChatSendDTO dto = new AiChatSendDTO();
        dto.setSessionId(5L);
        dto.setPrompt("灯不亮了");

        AiChatMessageVO vo = aiChatService.sendMessage(1L, dto);

        assertNotNull(vo.getContent());
        assertEquals(AiChatMessage.ROLE_ASSISTANT, vo.getRole());
    }

    /**
     * 构建上下文时应包含 system 提示。
     */
    @Test
    void sendMessage_contextIncludesSystemPrompt() {
        when(llmClient.chat(anyList(), any())).thenReturn("ok");

        AiChatSendDTO dto = new AiChatSendDTO();
        dto.setPrompt("测试");
        aiChatService.sendMessage(1L, dto);

        ArgumentCaptor<List<ChatMessage>> ctxCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).chat(ctxCaptor.capture(), any());
        List<ChatMessage> context = ctxCaptor.getValue();
        assertEquals(ChatMessage.ROLE_SYSTEM, context.get(0).getRole());
        assertEquals(ChatMessage.ROLE_USER, context.get(context.size() - 1).getRole());
    }

    /**
     * 查询消息列表应校验会话归属。
     */
    @Test
    void listMessages_notOwner_throwsForbidden() {
        AiChatSession session = new AiChatSession();
        session.setId(7L);
        session.setUserId(3L);
        when(sessionMapper.selectById(7L)).thenReturn(session);

        BizException ex = assertThrows(BizException.class,
                () -> aiChatService.listMessages(1L, 7L));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    /**
     * 查询会话列表应按更新时间倒序返回。
     */
    @Test
    void listSessions_returnsOwnedSessions() {
        AiChatSession s1 = new AiChatSession();
        s1.setId(1L);
        s1.setUserId(1L);
        s1.setTitle("会话1");
        s1.setUpdateTime(LocalDateTime.now());
        AiChatSession s2 = new AiChatSession();
        s2.setId(2L);
        s2.setUserId(1L);
        s2.setTitle("会话2");
        s2.setUpdateTime(LocalDateTime.now().minusHours(1));
        when(sessionMapper.selectList(any())).thenReturn(Arrays.asList(s1, s2));

        List<AiChatSessionVO> list = aiChatService.listSessions(1L);

        assertEquals(2, list.size());
        assertEquals("会话1", list.get(0).getTitle());
    }
}
