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

---

## 一、后端代码

### 1.1 pom.xml（完整文件）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.apartment</groupId>
    <artifactId>apartment-hub-server</artifactId>
    <version>1.0.0</version>
    <name>ApartmentHub Server</name>
    <description>Chain Apartment Management System Backend</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jjwt.version>0.12.3</jjwt.version>
        <easyexcel.version>3.3.4</easyexcel.version>
        <springdoc.version>2.3.0</springdoc.version>
        <spring-ai.version>1.0.0-M5</spring-ai.version>        <!-- 新增 -->
    </properties>

    <repositories>                                              <!-- 新增：Spring Milestones 仓库 -->
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring AOP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- MinIO -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.7</version>
        </dependency>

        <!-- EasyExcel -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>${easyexcel.version}</version>
        </dependency>

        <!-- SpringDoc OpenAPI (Swagger) -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Spring AI - OpenAI Starter (兼容 DeepSeek) -->    <!-- 新增 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>

        <!-- WebSocket -->                                       <!-- 新增 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**改动说明：**

| 改动 | 作用 |
|------|------|
| `<spring-ai.version>1.0.0-M5</spring-ai.version>` | 统一管理 Spring AI 版本号 |
| `<repositories>` 添加 Spring Milestones | Spring AI M5 是里程碑版本，需要从 Spring 里程碑仓库下载 |
| `spring-ai-openai-spring-boot-starter` | Spring AI 的 OpenAI 兼容 Starter，DeepSeek 的 API 与 OpenAI 兼容，所以用这个 |
| `spring-boot-starter-websocket` | Spring Boot 原生 WebSocket 支持 |

### 1.2 application.yml（完整文件）

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/apartment_hub?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:123456}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
  cache:
    type: redis
    redis:
      time-to-live: 300000
      cache-null-values: false
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
  ai:                                                      # 新增：Spring AI 配置
    openai:
      api-key: ${AI_API_KEY:your-deepseek-api-key}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  type-enums-package: com.apartment.hub.enums
  mapper-locations: classpath*:/mapper/**/*.xml

jwt:
  secret: ${JWT_SECRET:ApartmentHub2026SecretKeyForJWTTokenGenerationMustBe256Bits!!}
  expiration: ${JWT_EXPIRATION:86400000}

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

minio:
  endpoint: http://127.0.0.1:9000
  access-key: admin
  secret-key: 12345678
  bucket: room
```

**AI 配置详解：**

| 配置项 | 说明 |
|--------|------|
| `spring.ai.openai.api-key` | DeepSeek API Key，通过环境变量 `AI_API_KEY` 传入，避免硬编码 |
| `spring.ai.openai.base-url` | DeepSeek API 地址。Spring AI 会自动拼接 `/v1/chat/completions`，所以这里写 `https://api.deepseek.com` 即可 |
| `spring.ai.openai.chat.options.model` | 使用 `deepseek-chat` 模型（DeepSeek-V3） |
| `spring.ai.openai.chat.options.temperature` | 0.7，控制回复的随机性（0=确定，1=创意） |

> **为什么用 OpenAI Starter 配 DeepSeek？** DeepSeek 的 API 与 OpenAI 完全兼容（`/v1/chat/completions` 端点、同样的请求/响应格式），所以 Spring AI 的 OpenAI Starter 不用做任何修改就能对接 DeepSeek。

### 1.3 SecurityConfig.java（改动：1 行）

```java
package com.apartment.hub.config;

import com.apartment.hub.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.apartment.hub.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/uploads/**", "/api/images/**", "/api/pay/confirm/**", "/ws/**").permitAll()  // ← 这行加了 "/ws/**"
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "Unauthorized")));
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write(objectMapper.writeValueAsString(Result.fail(403, "Access denied")));
                        })
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**为什么 `/ws/**` 要 permitAll？** WebSocket 握手是一次 HTTP Upgrade 请求，它也会经过 Spring Security 的 Filter Chain。如果不在 SecurityConfig 里放行，JWT 认证过滤器会拦截这个请求。真正的认证由 WebSocket 的 HandshakeInterceptor 完成（见下一节）。

### 1.4 WebSocketConfig.java（新建）

```java
package com.apartment.hub.config;

import com.apartment.hub.websocket.AiChatWebSocketHandler;
import com.apartment.hub.websocket.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket                                    // 开启 WebSocket 支持
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiChatWebSocketHandler aiChatHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiChatHandler, "/ws/ai/chat")   // WebSocket 端点路径
                .addInterceptors(jwtHandshakeInterceptor)    // 握手时执行 JWT 验证
                .setAllowedOrigins("*");                     // 允许跨域
    }
}
```

**关键点：**
- `@EnableWebSocket` 开启 Spring 的 WebSocket 支持
- `addInterceptors(jwtHandshakeInterceptor)` 在握手阶段拦截，验证 token
- `setAllowedOrigins("*")` 必须设置，否则浏览器跨域 WebSocket 连接会被拒绝

### 1.5 JwtHandshakeInterceptor.java（新建）

```java
package com.apartment.hub.websocket;

import com.apartment.hub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 1. 从 URL 查询参数中提取 token
        String query = request.getURI().getQuery();
        if (query == null || !query.contains("token=")) {
            log.warn("WebSocket handshake rejected: no token parameter");
            return false;   // 返回 false = 拒绝握手
        }

        String token = extractParam(query, "token");
        if (token == null || !jwtUtil.isTokenValid(token)) {
            log.warn("WebSocket handshake rejected: invalid token");
            return false;
        }

        // 2. 解析 token 中的用户信息
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            attributes.put("userId", userId);       // 存入 WebSocket session attributes
            attributes.put("username", username);    // 后续 Handler 可以读取
            log.info("WebSocket handshake success: user={}", username);
            return true;   // 返回 true = 允许握手
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: token parse error", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手成功后不做额外处理
    }

    private String extractParam(String query, String key) {
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return null;
    }
}
```

**为什么 token 要通过 URL 参数传递？**

WebSocket 握手过程：
```
浏览器 → HTTP GET /ws/ai/chat?token=xxx （携带 Upgrade 头）
        ↓
        这是唯一一次 HTTP 请求，可以用 URL 参数传 token
        ↓
服务器 → 101 Switching Protocols（握手成功）
        ↓
之后全走 WebSocket 帧，没有 HTTP Header，无法传 Authorization
```

所以 token 只能在握手时的 URL 参数中传递。浏览器原生 `WebSocket` API 不支持自定义请求头。

### 1.6 AiChatWebSocketHandler.java（新建，核心文件）

```java
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

    private final ChatModel chatModel;                              // Spring AI 自动注入
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

    // ==================== 1. 连接建立 ====================
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        sessions.put(session.getId(), session);
        log.info("AI chat WebSocket connected: session={}, user={}", session.getId(), username);
        sendMessage(session, Map.of("type", "system", "content",
                "你好，" + username + "！我是公寓管家小安，有什么可以帮你的？"));
    }

    // ==================== 2. 接收消息 → 调用 AI → 流式返回 ====================
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

            // 通知前端"开始生成"（前端会创建一个空消息占位）
            sendMessage(session, Map.of("type", "start"));

            // 构建 Prompt：系统提示词 + 用户问题
            Prompt prompt = new Prompt(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userMessage)
            );

            // 流式调用 DeepSeek API
            chatModel.stream(prompt)
                    .doOnNext(chatResponse -> {
                        // 每个 token 到达时立刻推送给前端
                        String content = chatResponse.getResult().getOutput().getContent();
                        if (content != null && !content.isEmpty()) {
                            sendMessage(session, Map.of("type", "token", "content", content));
                        }
                    })
                    .doOnComplete(() ->
                        // 所有 token 发送完毕，通知前端
                        sendMessage(session, Map.of("type", "done"))
                    )
                    .doOnError(e -> {
                        log.error("AI chat error for session={}", session.getId(), e);
                        sendMessage(session, Map.of("type", "error", "content",
                                "抱歉，AI 服务暂时不可用，请稍后再试。"));
                    })
                    .blockLast();   // 阻塞当前线程，等待流结束（WebSocket 的 handleTextMessage 本身就运行在独立线程中）

        } catch (Exception e) {
            log.error("Failed to process chat message", e);
            sendMessage(session, Map.of("type", "error", "content", "处理消息时出错，请重试。"));
        }
    }

    // ==================== 3. 连接关闭 ====================
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

    // ==================== 4. 发送消息工具方法 ====================
    private void sendMessage(WebSocketSession session, Object data) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(data);
                synchronized (session) {                            // 加锁，避免并发发送导致帧乱序
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket message", e);
        }
    }
}
```

**核心流程解读：**

```
handleTextMessage 被调用
        │
        ▼
解析 JSON → 提取 "message" 字段
        │
        ▼
发送 {"type":"start"} （告诉前端准备接收）
        │
        ▼
构建 Prompt = SystemMessage(角色定义) + UserMessage(用户问题)
        │
        ▼
chatModel.stream(prompt)   ← 返回 Flux<ChatResponse>，Spring AI 底层用 HTTP SSE 调用 DeepSeek
        │
        ├─ .doOnNext()   → 每收到一个 token，立即 WebSocket 推送给前端
        ├─ .doOnComplete() → 发送 {"type":"done"}
        └─ .doOnError()    → 发送 {"type":"error"}
        │
        ▼
.blockLast()   ← 阻塞直到 Flux 结束（必须，否则 handleTextMessage 提前返回）
```

**为什么用 `.blockLast()`？**
- `chatModel.stream()` 返回的是 `Flux`（响应式流），默认是非阻塞的
- 如果不调用 `.blockLast()`，`handleTextMessage` 方法会立即返回，Spring 会在 Flux 完成前就释放资源
- `.blockLast()` 会让当前线程等待，直到流结束才继续执行

---

## 二、前端代码

### 2.1 vite.config.ts（完整文件）

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: ['.trycloudflare.com'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {                                    // 新增：WebSocket 代理
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true                                  // 关键：开启 WebSocket 协议代理
      }
    }
  }
})
```

**`ws: true` 的作用：** Vite 开发服务器的 HTTP 代理默认只代理 HTTP 请求。开启 `ws: true` 后，代理会同时处理 WebSocket Upgrade 请求，把浏览器的 WebSocket 连接转发到后端 8080 端口。

### 2.2 layout/index.vue（改动：2 行）

改动很小，只在两处加了代码：

```vue
<template>
  <el-container class="layout-container">
    <!-- ... 原有布局 ... -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
    <AiChat />                                     <!-- ← 新增：AI 聊天组件 -->
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { constantRoutes } from '@/router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { RouteRecordRaw } from 'vue-router'
import AiChat from '@/components/AiChat.vue'        <!-- ← 新增：导入组件 -->
// ... 其余不变 ...
</script>
```

`<AiChat />` 使用 `position: fixed` 定位在右下角，与页面布局无耦合，放在布局模板最外层即可。

### 2.3 AiChat.vue（新建，完整文件）

#### 模板（UI 结构）

```vue
<template>
  <div class="ai-chat-container">
    <!-- 浮动按钮 -->
    <div v-if="!open" class="ai-chat-fab" @click="openChat">
      <span class="ai-chat-fab-icon">🤖</span>
    </div>

    <!-- 聊天窗口 -->
    <Transition name="slide-up">
      <div v-if="open" class="ai-chat-window">
        <div class="ai-chat-header">
          <div class="ai-chat-header-left">
            <span class="ai-chat-avatar">🤖</span>
            <div>
              <div class="ai-chat-title">AI 公寓管家</div>
              <div class="ai-chat-subtitle">{{ connected ? '在线' : '连接中...' }}</div>
            </div>
          </div>
          <div class="ai-chat-header-right">
            <el-button text circle size="small" @click="clearChat">
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button text circle size="small" @click="open = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="ai-chat-body" ref="bodyRef">
          <!-- 空状态：建议问题 -->
          <div v-if="messages.length === 0" class="ai-chat-empty">
            <div class="ai-chat-empty-icon">🏠</div>
            <div class="ai-chat-empty-text">问我任何公寓管理相关的问题</div>
            <div class="ai-chat-suggestions">
              <div
                v-for="q in suggestions"
                :key="q"
                class="ai-chat-suggestion"
                @click="sendMessage(q)"
              >{{ q }}</div>
            </div>
          </div>

          <!-- 消息列表 -->
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="ai-chat-message"
            :class="msg.role"
          >
            <div class="ai-chat-message-avatar">
              {{ msg.role === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="ai-chat-message-content">
              <div class="ai-chat-message-text" v-text="msg.content"></div>
              <span v-if="msg.streaming" class="ai-chat-cursor">|</span>   <!-- 流式光标 -->
            </div>
          </div>
        </div>

        <div class="ai-chat-footer">
          <el-input
            v-model="input"
            placeholder="输入您的问题..."
            :disabled="loading"
            @keyup.enter="sendMessage()"
          >
            <template #append>
              <el-button
                :icon="loading ? undefined : Promotion"
                :loading="loading"
                :disabled="!input.trim()"
                @click="sendMessage()"
              />
            </template>
          </el-input>
        </div>
      </div>
    </Transition>
  </div>
</template>
```

#### 脚本（核心逻辑）

```typescript
<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Promotion, Delete, Close } from '@element-plus/icons-vue'

interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  streaming?: boolean      // 是否正在流式输出（显示闪烁光标）
}

const open = ref(false)          // 聊天窗口是否打开
const input = ref('')            // 输入框内容
const loading = ref(false)       // 是否正在等待 AI 回复
const connected = ref(false)     // WebSocket 是否已连接
const messages = ref<ChatMessage[]>([])
const bodyRef = ref<HTMLElement>()

let ws: WebSocket | null = null
let reconnectTimer: number | null = null

const suggestions = [
  'A-102房间有人住吗',
  '下个月到期的合同有哪些',
  '本月租金收入多少',
  '帮我查一下欠费情况',
]

// ==================== 1. WebSocket 连接 ====================
function getToken(): string {
  return localStorage.getItem('token') || ''
}

function getWsUrl(): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = location.host
  return `${protocol}//${host}/ws/ai/chat?token=${getToken()}`
  // 开发环境：ws://localhost:5174/ws/ai/chat?token=xxx   （Vite 代理到 8080）
  // 生产环境：wss://域名/ws/ai/chat?token=xxx              （Nginx 代理到 8080）
}

function connect() {
  if (ws && ws.readyState === WebSocket.OPEN) return

  try {
    ws = new WebSocket(getWsUrl())

    ws.onopen = () => {
      connected.value = true
      loading.value = false
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleMessage(data)
      } catch {
        // ignore parse errors
      }
    }

    ws.onclose = () => {
      connected.value = false
      if (open.value) {
        reconnectTimer = window.setTimeout(connect, 3000)  // 3 秒后自动重连
      }
    }

    ws.onerror = () => {
      connected.value = false
    }
  } catch {
    connected.value = false
  }
}

// ==================== 2. 消息处理（核心：流式显示） ====================
function handleMessage(data: any) {
  switch (data.type) {
    case 'system':
      messages.value.push({ role: 'system', content: data.content })
      scrollToBottom()
      break

    case 'start':
      loading.value = true
      // 创建一个空消息，标记 streaming=true，后续 token 会拼接到这个 content 上
      messages.value.push({ role: 'assistant', content: '', streaming: true })
      scrollToBottom()
      break

    case 'token':
      // 找到最后一条消息，追加 token 内容
      if (messages.value.length > 0) {
        const last = messages.value[messages.value.length - 1]
        if (last.streaming) {
          last.content += data.content    // Vue3 响应式：content 变化自动更新 DOM
        }
      }
      scrollToBottom()
      break

    case 'done':
      if (messages.value.length > 0) {
        messages.value[messages.value.length - 1].streaming = false  // 去掉光标
      }
      loading.value = false
      scrollToBottom()
      break

    case 'error':
      if (messages.value.length > 0) {
        messages.value[messages.value.length - 1].streaming = false
      }
      messages.value.push({ role: 'system', content: data.content || '出错了' })
      loading.value = false
      scrollToBottom()
      break
  }
}

// ==================== 3. 发送消息 ====================
function sendMessage(text?: string) {
  const msg = text || input.value.trim()
  if (!msg || loading.value) return

  if (!connected.value) {
    messages.value.push({ role: 'system', content: '正在连接 AI 服务，请稍后再试...' })
    connect()
    return
  }

  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true
  scrollToBottom()

  ws?.send(JSON.stringify({ message: msg }))
}

function clearChat() {
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  })
}

function openChat() {
  open.value = true
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    connect()
  }
}

watch(open, (val) => {
  if (!val) {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }
})
</script>
```

#### 流式显示原理（关键）

```
用户点击发送
    │
    ▼
发送 {"message":"A-102有人住吗"} 到 WebSocket
    │
    ▼
收到 {"type":"start"}
    → messages.push({ role:'assistant', content:'', streaming:true })
    → 界面出现一个空的 AI 消息气泡 + 闪烁光标 |
    │
    ▼
收到 {"type":"token", content:"A-102"}
    → last.content += "A-102"
    → Vue 响应式更新，气泡里显示 "A-102"
    │
    ▼
收到 {"type":"token", content:"房间目前"}
    → last.content += "房间目前"
    → 气泡里显示 "A-102房间目前"
    │
    ▼  ... 持续追加 ...
    │
    ▼
收到 {"type":"done"}
    → last.streaming = false
    → 光标消失，loading 状态解除
```

#### 样式（CSS）

```css
<style scoped>
.ai-chat-container {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}

/* 浮动按钮 */
.ai-chat-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.ai-chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(37, 99, 235, 0.55);
}
.ai-chat-fab-icon {
  font-size: 24px;
  line-height: 1;
}

/* 聊天窗口 */
.ai-chat-window {
  width: 400px;
  height: 560px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 弹出动画 */
.slide-up-enter-active { transition: all 0.3s ease-out; }
.slide-up-leave-active { transition: all 0.2s ease-in; }
.slide-up-enter-from { opacity: 0; transform: translateY(20px) scale(0.95); }
.slide-up-leave-to { opacity: 0; transform: translateY(10px) scale(0.98); }

/* Header */
.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
}
.ai-chat-header-left { display: flex; align-items: center; gap: 10px; }
.ai-chat-avatar { font-size: 28px; line-height: 1; }
.ai-chat-title { font-size: 15px; font-weight: 600; }
.ai-chat-subtitle { font-size: 12px; opacity: 0.8; }
.ai-chat-header-right { display: flex; gap: 4px; }
.ai-chat-header-right .el-button { color: #fff; }

/* Body */
.ai-chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}
.ai-chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 60px;
}
.ai-chat-empty-icon { font-size: 48px; margin-bottom: 12px; }
.ai-chat-empty-text { font-size: 14px; color: #64748b; margin-bottom: 20px; }
.ai-chat-suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.ai-chat-suggestion {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}
.ai-chat-suggestion:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

/* 消息气泡 */
.ai-chat-message { display: flex; gap: 8px; margin-bottom: 14px; }
.ai-chat-message.user { flex-direction: row-reverse; }        /* 用户消息靠右 */
.ai-chat-message-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.ai-chat-message-content { max-width: 75%; }
.ai-chat-message-text {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-chat-message.user .ai-chat-message-text {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-chat-message.assistant .ai-chat-message-text {
  background: #fff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 4px;
}
.ai-chat-message.system .ai-chat-message-text {
  background: #fef3c7;
  color: #92400e;
  font-size: 13px;
  text-align: center;
  border-radius: 10px;
}
.ai-chat-message.system { justify-content: center; }
.ai-chat-message.system .ai-chat-message-avatar { display: none; }

/* 流式光标 */
.ai-chat-cursor {
  display: inline;
  animation: blink 1s infinite;
  color: #2563eb;
  font-weight: bold;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Footer */
.ai-chat-footer { padding: 12px 16px; border-top: 1px solid #e2e8f0; }
.ai-chat-footer :deep(.el-input-group__append) {
  background: #2563eb;
  border-color: #2563eb;
  padding: 0 10px;
}
.ai-chat-footer :deep(.el-input-group__append .el-button) { color: #fff; }
</style>
```

---

## 三、消息协议

### 前端 → 后端

```json
{"message": "用户问题"}
```

### 后端 → 前端

| type | 含义 | 触发时机 |
|------|------|----------|
| `system` | 系统消息 | 连接成功时发欢迎语 / 错误提示 |
| `start` | 开始生成 | AI 收到问题，准备开始回复 |
| `token` | 文字片段 | 流式返回，每个 token 一条消息 |
| `done` | 生成完成 | 所有 token 发送完毕 |
| `error` | 错误消息 | AI 调用失败或处理异常 |

完整时序：

```json
// 1. 连接建立
← {"type": "system", "content": "你好，张三！我是公寓管家小安，有什么可以帮你的？"}

// 2. 用户发问
→ {"message": "A-102有人住吗"}

// 3. AI 开始回复
← {"type": "start"}

// 4. 流式返回
← {"type": "token", "content": "A-102"}
← {"type": "token", "content": "房间"}
← {"type": "token", "content": "目前有人住，"}
← {"type": "token", "content": "租客是张三。"}

// 5. 完成
← {"type": "done"}

// 6. 如果出错
← {"type": "error", "content": "抱歉，AI 服务暂时不可用，请稍后再试。"}
```

---

## 四、部署步骤

### 1. 获取 DeepSeek API Key

去 [platform.deepseek.com](https://platform.deepseek.com) 注册，在 API Keys 页面创建 key。新用户有免费额度（约 500 万 tokens）。

### 2. 配置环境变量

```bash
export AI_API_KEY=sk-xxxxxxxxxxxxxxxx
```

或者在 `application.yml` 中把 `your-deepseek-api-key` 替换成真实 key（**不要提交到 git**）。

### 3. 启动

```bash
# 终端1：后端
cd apartment-hub-server
mvn spring-boot:run

# 终端2：前端
cd apartment-hub-web
npm run dev
```

### 4. 使用

1. 浏览器访问 `http://localhost:5173`（如果端口被占会自动跳到 5174）
2. 登录系统
3. 点击右下角 🤖 按钮
4. 输入问题或点击建议问题开始对话

---

## 五、当前局限

第一阶段是**纯对话模式**，AI 没有接入数据库。所有回答基于通用知识 + 系统提示词，查不了真实数据。

| 能做的 | 不能做的（第二阶段） |
|--------|---------------------|
| 回答公寓管理常识 | 查询 A-102 的真实入住状态 |
| 解释合同条款概念 | 查看张三的实际欠费金额 |
| 给出管理建议 | 生成真实催租通知 |
| 理解用户意图 | 执行数据库操作 |

---

## 六、文件清单

```
apartment-hub-server/
├── pom.xml                                         # 改：+spring-ai, +websocket 依赖, +milestones 仓库
├── src/main/resources/application.yml              # 改：+spring.ai.openai 配置块
└── src/main/java/com/apartment/hub/
    ├── config/
    │   ├── SecurityConfig.java                     # 改：第39行 +"/ws/**" permitAll
    │   └── WebSocketConfig.java                    # 新建
    └── websocket/
        ├── JwtHandshakeInterceptor.java             # 新建
        └── AiChatWebSocketHandler.java              # 新建（核心）

apartment-hub-web/
├── vite.config.ts                                  # 改：+"/ws" 代理配置
└── src/
    ├── layout/index.vue                            # 改：+2行（import + <AiChat />）
    └── components/AiChat.vue                       # 新建（~460行）
```
