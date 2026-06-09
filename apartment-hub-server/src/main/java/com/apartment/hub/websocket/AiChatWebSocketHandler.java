package com.apartment.hub.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            你是公寓管理系统的 AI 助理，名叫"小安"。你的职责是帮助用户管理公寓业务，包括：
            - 查询房间入住状态
            - 查询合同信息（到期时间、租客等）
            - 查询账单和缴费情况
            - 回答公寓管理相关的一般性问题

            回复要求：
            - 用中文回答，语气友好专业
            - 如果用户询问开放式问题，可以给出建议
            - 如果被问到系统暂不支持的功能，如实告知并提供替代方案
            - 回复控制在200字以内，简洁明了
            """;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        sessions.put(session.getId(), session);
        log.info("AI chat WebSocket connected: session={}, user={}", session.getId(), username);
        sendMessage(session, Map.of("type", "system", "content",
                "你好，" + username + "！我是公寓管家小安，有什么可以帮你的？"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("AI chat message from session={}: {}", session.getId(), payload);

        try {
            Map<?, ?> msgMap = objectMapper.readValue(payload, Map.class);
            String userMessage = (String) msgMap.get("message");
            if (userMessage == null || userMessage.isBlank()) {
                sendMessage(session, Map.of("type", "error", "content", "消息不能为空"));
                return;
            }

            // Send start marker
            sendMessage(session, Map.of("type", "start"));

            // Build prompt with system context
            Prompt prompt = new Prompt(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userMessage)
            );

            // Stream response
            chatModel.stream(prompt)
                    .doOnNext(chatResponse -> {
                        String content = chatResponse.getResult().getOutput().getContent();
                        if (content != null && !content.isEmpty()) {
                            sendMessage(session, Map.of("type", "token", "content", content));
                        }
                    })
                    .doOnComplete(() -> sendMessage(session, Map.of("type", "done")))
                    .doOnError(e -> {
                        log.error("AI chat error for session={}", session.getId(), e);
                        sendMessage(session, Map.of("type", "error", "content",
                                "抱歉，AI 服务暂时不可用，请稍后再试。"));
                    })
                    .blockLast();

        } catch (Exception e) {
            log.error("Failed to process chat message", e);
            sendMessage(session, Map.of("type", "error", "content", "处理消息时出错，请重试。"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("AI chat WebSocket disconnected: session={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        log.error("AI chat WebSocket transport error: session={}", session.getId(), exception);
    }

    private void sendMessage(WebSocketSession session, Object data) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(data);
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket message", e);
        }
    }
}
