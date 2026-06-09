package com.apartment.hub.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            你是公寓管理系统的 AI 助理，名叫"小安"。你可以通过调用工具函数查询真实的业务数据来回答用户问题。

            你可以查询以下信息：
            - 房间状态：某房间是否有人住、租客是谁、合同到期时间
            - 逾期账单：哪些租客欠费、欠多少、欠了多久
            - 即将到期的合同：未来N天内有哪些合同要到期
            - 收入统计：本月租金收入、待收金额
            - 租客信息：某租客住哪个房间、合同情况、是否欠费

            规则：
            - 用户问数据相关的问题时，必须调用工具查询，不要编造答案
            - 用中文回答，语气友好专业
            - 如果查询结果为空或未找到，如实告知用户
            - 回复控制在300字以内，简洁明了
            """;

    public AiChatWebSocketHandler(ChatModel chatModel, ObjectMapper objectMapper,
                                   List<FunctionCallback> functionCallbacks) {
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultFunctions(functionCallbacks.toArray(new FunctionCallback[0]))
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        sessions.put(session.getId(), session);
        log.info("AI chat WebSocket connected: session={}, user={}", session.getId(), username);
        sendMessage(session, Map.of("type", "system", "content",
                "你好，" + username + "！我是公寓管家小安，现在可以查询真实数据了，有什么可以帮你的？"));
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

            sendMessage(session, Map.of("type", "start"));

            chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .stream()
                    .chatResponse()
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
