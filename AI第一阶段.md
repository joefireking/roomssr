# AI 第一阶段：AI 公寓管家（纯对话）

## 概述

在公寓管理系统右下角添加 AI 助手浮窗，用户可以通过自然语言提问，AI 流式回复。基于 Spring AI + DeepSeek + WebSocket 实现。

## 架构

```
浏览器 (Vue3)                    后端 (SpringBoot)                   AI服务
    │                                │                                  │
    │  ws://host/ws/ai/chat?token=xx │                                  │
    │ ──────────────────────────────>│  JwtHandshakeInterceptor 验token │
    │                                │                                  │
    │  {"type":"system","content":""}│                                  │
    │ <──────────────────────────────│  连接成功，发送欢迎消息            │
    │                                │                                  │
    │  {"message":"A-102有人住吗"}    │                                  │
    │ ──────────────────────────────>│  AiChatWebSocketHandler           │
    │                                │    ├─ 构建 Prompt (系统提示词+用户问题)
    │                                │    ├─ ChatModel.stream(prompt)    │
    │                                │    │  ───────────────────────────>│ HTTP Stream
    │                                │    │                              │ DeepSeek API
    │                                │    │  <───────────────────────────│ 逐token返回
    │  {"type":"token","content":"A"}│    │                              │
    │ <──────────────────────────────│  流式推送给前端                    │
    │  {"type":"token","content":"-"}│                                  │
    │ <──────────────────────────────│                                  │
    │  {"type":"done"}                │                                  │
    │ <──────────────────────────────│  生成完成                         │
```

## 技术栈

| 层级 | 技术 |
|------|------|
| AI框架 | Spring AI 1.0.0-M5 (OpenAI Starter) |
| 大模型 | DeepSeek (deepseek-chat) |
| 通信协议 | WebSocket (raw, 非STOMP) |
| 流式输出 | Spring AI `ChatModel.stream()` → Flux\<ChatResponse\> |
| 前端 | Vue3 + Element Plus + 原生 WebSocket API |

## 后端改动

### 1. 添加依赖 (pom.xml)

```xml
<!-- Spring Milestones 仓库 (Spring AI M5 需要) -->
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
    </repository>
</repositories>

<!-- Spring AI - OpenAI Starter (兼容 DeepSeek) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M5</version>
</dependency>

<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 2. DeepSeek 配置 (application.yml)

```yaml
spring:
  ai:
    openai:
      api-key: ${AI_API_KEY:your-deepseek-api-key}
      base-url: https://api.deepseek.com     # Spring AI 自动拼接 /v1/chat/completions
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
```

### 3. WebSocket 配置 (config/WebSocketConfig.java)

```java
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiChatWebSocketHandler aiChatHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiChatHandler, "/ws/ai/chat")
                .addInterceptors(jwtHandshakeInterceptor)  // 握手时验证JWT
                .setAllowedOrigins("*");
    }
}
```

### 4. JWT 握手拦截器 (websocket/JwtHandshakeInterceptor.java)

- 从 URL 参数 `?token=xxx` 提取 JWT
- 验证 token 有效性
- 将 userId 和 username 存入 WebSocket session attributes

**为什么不用 HTTP Header？** WebSocket 握手是一次性 HTTP 升级请求，后续帧没有 Header 概念。token 只能通过 URL 参数传递。

### 5. AI 聊天处理器 (websocket/AiChatWebSocketHandler.java)

核心逻辑：

```java
@Component
@RequiredArgsConstructor
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatModel chatModel;  // Spring AI 自动注入

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 1. 解析用户消息
        Map<?, ?> msgMap = objectMapper.readValue(payload, Map.class);
        String userMessage = (String) msgMap.get("message");

        // 2. 构建 Prompt（系统提示词 + 用户消息）
        Prompt prompt = new Prompt(
            new SystemMessage(SYSTEM_PROMPT),  // 定义 AI 角色
            new UserMessage(userMessage)
        );

        // 3. 流式调用 DeepSeek
        chatModel.stream(prompt)
            .doOnNext(response -> {
                String content = response.getResult().getOutput().getContent();
                sendMessage(session, Map.of("type", "token", "content", content));
            })
            .doOnComplete(() -> sendMessage(session, Map.of("type", "done")))
            .doOnError(e -> sendMessage(session, Map.of("type", "error", ...)))
            .blockLast();  // 阻塞直到流结束
    }
}
```

### 6. SecurityConfig 放行

```java
.requestMatchers("/ws/**").permitAll()
```

WebSocket 的认证由 `JwtHandshakeInterceptor` 负责，不走 Spring Security 的 Filter Chain。

## 前端改动

### 1. Vite 代理配置 (vite.config.ts)

```typescript
proxy: {
    '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true   // 关键：开启 WebSocket 代理
    }
}
```

### 2. AiChat.vue 组件结构

```
┌─────────────────────────────────┐
│  🤖 AI 公寓管家          在线   │ ← header
├─────────────────────────────────┤
│                                 │
│  💡 快捷问题建议                 │ ← 空状态
│  ┌─────────────────────┐        │
│  │ A-102房间有人住吗     │        │
│  │ 下个月到期的合同有哪些 │        │
│  │ 本月租金收入多少      │        │
│  └─────────────────────┘        │
│                                 │
│  👤 A-102房间有人住吗           │ ← 用户消息
│  🤖 A-102房间目前...            │ ← AI回复（流式）│
│                                 │
├─────────────────────────────────┤
│  [输入您的问题...        ] [➤]  │ ← footer
└─────────────────────────────────┘
```

**生命周期：**

```
打开聊天 → new WebSocket(url?token=xxx)
         → ws.onopen → connected = true
         → ws.onmessage → 根据 type 更新消息列表
         → 关闭聊天 → ws.close()

异常断线 → ws.onclose → 3秒后自动重连（仅聊天窗口打开时）
```

**流式显示原理：**

```
收到 {"type":"start"}   → 创建空消息 msg = {content:"", streaming:true}
收到 {"type":"token", content:"A"}  → msg.content += "A"  (视图响应式更新)
收到 {"type":"token", content:"-"}  → msg.content += "-"
收到 {"type":"done"}    → msg.streaming = false  (去掉光标)
```

### 3. 布局集成 (layout/index.vue)

```vue
<el-container>
  <!-- 原有布局 -->
  <AiChat />  <!-- 加到最外层容器内 -->
</el-container>
```

组件用 `position: fixed; right: 24px; bottom: 24px; z-index: 2000` 固定在右下角，与页面内容互不影响。

## 消息协议

### 前端 → 后端

```json
{"message": "用户问题"}
```

### 后端 → 前端

| type | 含义 | 触发时机 |
|------|------|----------|
| `system` | 系统消息 | 连接成功 / 错误提醒 |
| `start` | 开始生成 | AI 开始思考 |
| `token` | 文字片段 | 流式返回，每个 token 一个 |
| `done` | 生成完成 | 流结束 |
| `error` | 错误消息 | AI 调用失败 |

```json
{"type": "system", "content": "你好，张三！我是公寓管家小安..."}
{"type": "start"}
{"type": "token", "content": "根据"}
{"type": "token", "content": "当前"}
{"type": "token", "content": "信息"}
{"type": "done"}
{"type": "error", "content": "AI 服务暂时不可用，请稍后再试。"}
```

## 系统提示词

AI 的角色由 `AiChatWebSocketHandler` 中的 `SYSTEM_PROMPT` 定义：

```text
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
```

## 部署步骤

### 1. 获取 DeepSeek API Key

去 [platform.deepseek.com](https://platform.deepseek.com) 注册，在 API Keys 页面创建 key。新用户有免费额度。

### 2. 配置环境变量

```bash
export AI_API_KEY=sk-xxxxxxxxxxxxxxxx
```

或者直接改 `application.yml` 中的 `your-deepseek-api-key`。

### 3. 启动

```bash
# 后端
cd apartment-hub-server
mvn spring-boot:run

# 前端
cd apartment-hub-web
npm run dev
```

### 4. 使用

1. 浏览器访问 `http://localhost:5173`
2. 登录系统
3. 点击右下角 🤖 按钮
4. 开始提问

## 当前局限

第一阶段是**纯对话模式**，AI 没有接入数据库。所有回答基于通用知识和系统提示词，查不了真实的房间状态、合同、账单。

| 能做的 | 不能做的（第二阶段实现） |
|--------|------------------------|
| 回答公寓管理常识问题 | 查询 A-102 的真实入住状态 |
| 解释合同条款概念 | 查看张三的实际欠费金额 |
| 给出公寓管理建议 | 生成真实的催租通知 |
| 理解用户意图 | 操作数据库（创建合同等） |

## 第二阶段预告

**Tool Calling**：让 AI 能调用后端 Service 层。

- 定义 Function（如 `getRoomStatus`、`getOverdueBills`）
- DeepSeek 识别用户意图后返回 function_call
- 后端执行对应 Service 方法
- 将真实数据注入 AI 回复

```text
用户: "A-102有人住吗"
  ↓
AI 识别意图 → 调用 getRoomStatus("A-102")
  ↓
返回: A-102 当前有人入住，租客张三，合同到期 2026-12-31
  ↓
AI 组织语言回复用户
```

---

## 文件清单

```
apartment-hub-server/
├── pom.xml                                    # +3 依赖, +1 仓库
├── src/main/resources/application.yml         # +spring.ai.openai 配置
└── src/main/java/com/apartment/hub/
    ├── config/
    │   ├── SecurityConfig.java               # +/ws/** permitAll
    │   └── WebSocketConfig.java              # 新建
    └── websocket/
        ├── JwtHandshakeInterceptor.java       # 新建
        └── AiChatWebSocketHandler.java        # 新建

apartment-hub-web/
├── vite.config.ts                             # +/ws 代理
└── src/
    ├── layout/index.vue                       # +<AiChat /> 组件引入
    └── components/AiChat.vue                  # 新建
```
