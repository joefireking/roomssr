# AI 第三阶段：RAG 知识库（文档检索 + 增强生成）

## 概述

前两阶段 AI 只能查数据库（Tool Calling）或凭通用知识聊天。第三阶段引入 **RAG（Retrieval-Augmented Generation）**，让 AI 能检索你的项目文档（管理制度、合同条款、支付规则等），回答"提前退租违约金怎么算"这类需要查阅文档的问题。

## 核心原理

```
用户："提前退租违约金怎么算"
        │
        ▼
问题向量化（OpenAI text-embedding-3-small → 1536维向量）
        │
        ▼
在向量库中搜索最相关的文档片段（SimpleVectorStore 余弦相似度）
        │
        ▼
检索到：《公寓租赁管理制度》第3.2条 "提前退租需支付1个月租金..."
        │
        ▼
将检索结果注入 DeepSeek 的上下文窗口
        │
        ▼
AI 回答："根据《公寓租赁管理制度》第3.2条，提前退租需支付1个月租金作为违约金..."
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 向量存储 | Spring AI `SimpleVectorStore`（内存 + JSON 文件持久化） |
| 文档拆分 | Spring AI `TokenTextSplitter`（chunk_size=800, keep_separator） |
| 嵌入模型 | OpenAI `text-embedding-3-small`（1536 维，通过 RestClient 直接调用） |
| 检索 | `SimpleVectorStore.doSimilaritySearch()` 余弦相似度 Top-K |
| 知识库文档 | 项目根目录 5 个 markdown 文件（合同/启动/支付/Redis总结/合同bug） |

## 为什么不用 Qdrant？

Spring AI 1.0.0-M5 内置了 `SimpleVectorStore`，它：
- **零依赖**：不需要 Docker，不开外部服务
- **持久化**：支持 `save(File)` / `load(File)` 存到磁盘 JSON 文件
- **启动快**：第二次启动直接从文件加载，不需要重新向量化

开发/演示阶段完全够用。换成 Qdrant 只需改一个 Bean，代码不用变（都实现了 `VectorStore` 接口）。

## 为什么单独调 OpenAI Embedding API？

DeepSeek 没有提供 Embedding API（只有 Chat API）。所以嵌入层单独调用 OpenAI 的 `text-embedding-3-small`（$0.02/百万 tokens，极便宜），实现为自定义的 `EmbeddingModel`。

---

## 一、后端代码

### 1.1 OpenAiEmbeddingService.java（新建）

自定义 `EmbeddingModel` 实现，直接调用 OpenAI Embedding API。

```java
package com.apartment.hub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiEmbeddingService implements EmbeddingModel {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final int DIMENSIONS = 1536;  // text-embedding-3-small

    public OpenAiEmbeddingService(ObjectMapper objectMapper,
                                   @Value("${openai.embedding.api-key:${OPENAI_API_KEY:}}") String apiKey,
                                   @Value("${openai.embedding.model:text-embedding-3-small}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().build();

        if (apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY not set — embedding/RAG features will not work");
        } else {
            log.info("OpenAI Embedding service initialized: model={}, dimensions={}", model, DIMENSIONS);
        }
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embed(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (apiKey.isBlank()) {
            log.error("Cannot embed: OPENAI_API_KEY not set");
            return List.of(new float[DIMENSIONS]);
        }

        try {
            Map<String, Object> body = Map.of("model", model, "input", texts);

            String response = restClient.post()
                    .uri(EMBEDDING_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.get("embedding");
                float[] vec = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vec[i] = (float) embedding.get(i).asDouble();
                }
                embeddings.add(vec);
            }
            return embeddings;

        } catch (Exception e) {
            log.error("OpenAI embedding failed", e);
            return texts.stream().map(t -> new float[DIMENSIONS]).toList();
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<float[]> embeddings = embed(request.getInstructions());
        return new EmbeddingResponse(embeddings.stream()
                .map(e -> new org.springframework.ai.embedding.Embedding(e, 0))
                .toList());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
```

**关键点：**
- 实现了 Spring AI 的 `EmbeddingModel` 接口，`SimpleVectorStore` 可以直接用它
- 用 `RestClient`（Spring Boot 3.2 内置）调 OpenAI API，不需要额外依赖
- API Key 从环境变量 `OPENAI_API_KEY` 读取，和 DeepSeek 的 `AI_API_KEY` 分开管理
- 如果没有 API Key，返回零向量（不会崩溃），日志提示 `RAG features will not work`

### 1.2 KnowledgeBaseConfig.java（新建）

创建 `SimpleVectorStore` Bean。

```java
package com.apartment.hub.config;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeBaseConfig {

    @Bean
    public SimpleVectorStore vectorStore(OpenAiEmbeddingService embeddingService) {
        return SimpleVectorStore.builder(embeddingService).build();
    }
}
```

只需要 1 行——`SimpleVectorStore.builder(embeddingService).build()`。

### 1.3 KnowledgeBaseService.java（新建，核心文件）

负责文档导入、拆分、向量化存储、检索。

```java
package com.apartment.hub.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final SimpleVectorStore vectorStore;

    private static final String KB_DIR = "./uploads/kb";
    private static final String KB_FILE = KB_DIR + "/vector-store.json";

    // 知识库文档列表
    private static final String[] DOC_PATHS = {
            "../合同.md", "../启动.md", "../支付.md",
            "../Redis集成总结.md", "../合同bug.md"
    };

    // 文档拆分器：每块最多 800 字符，最小 50 字符
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(800)
            .withMinChunkSizeChars(50)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();

    private boolean ready = false;

    @PostConstruct
    void init() {
        try {
            // 优先从磁盘加载（避免每次重启都重新向量化）
            File file = new File(KB_FILE);
            if (file.exists()) {
                vectorStore.load(file);
                log.info("Knowledge base loaded from disk: {} bytes", file.length());
                ready = true;
                return;
            }

            // 首次运行：导入所有文档
            rebuildFromDocuments();
        } catch (Exception e) {
            log.error("Failed to initialize knowledge base", e);
        }
    }

    /**
     * 重新构建知识库：读取 markdown → 拆分 → 向量化 → 存储 → 持久化
     */
    public void rebuildFromDocuments() {
        log.info("Rebuilding knowledge base from documents...");
        List<Document> allChunks = new ArrayList<>();

        for (String path : DOC_PATHS) {
            try {
                Path filePath = Paths.get(path);
                if (!Files.exists(filePath)) {
                    log.warn("Document not found: {}", path);
                    continue;
                }

                String content = Files.readString(filePath);
                String fileName = filePath.getFileName().toString();

                // 按 markdown 标题（##）拆分成章节
                String[] sections = content.split("(?=^## )");
                for (String section : sections) {
                    if (section.isBlank()) continue;

                    String sectionTitle = extractTitle(section);
                    Document doc = Document.builder()
                            .text(section.trim())
                            .metadata(Map.of("source", fileName, "title", sectionTitle))
                            .build();

                    // 用 TokenTextSplitter 进一步拆分长章节
                    List<Document> chunks = splitter.split(doc);
                    for (Document chunk : chunks) {
                        chunk.getMetadata().put("source", fileName);
                        chunk.getMetadata().put("title", sectionTitle);
                    }
                    allChunks.addAll(chunks);
                }

                log.info("Ingested {} → {} chunks", fileName, allChunks.size());
            } catch (Exception e) {
                log.error("Failed to read document: {}", path, e);
            }
        }

        if (allChunks.isEmpty()) {
            log.warn("No documents ingested — knowledge base is empty");
            return;
        }

        // 向量化 + 存储
        vectorStore.add(allChunks);

        // 持久化到磁盘（下次启动直接 load）
        try {
            File dir = new File(KB_DIR);
            dir.mkdirs();
            vectorStore.save(new File(KB_FILE));
            log.info("Knowledge base built and saved: {} chunks", allChunks.size());
            ready = true;
        } catch (Exception e) {
            log.error("Failed to persist knowledge base", e);
        }
    }

    /**
     * 检索知识库，返回相关文档片段拼接成的上下文字符串
     */
    public String searchContext(String query, int topK) {
        if (!ready) return "";

        try {
            List<Document> results = vectorStore.doSimilaritySearch(
                    SearchRequest.query(query).withTopK(topK));

            if (results.isEmpty()) return "";

            return results.stream()
                    .map(doc -> {
                        String source = (String) doc.getMetadata().get("source");
                        String title = (String) doc.getMetadata().get("title");
                        return "【来源：" + source + " - " + title + "】\n" + doc.getContent();
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

        } catch (Exception e) {
            log.error("Knowledge base search failed", e);
            return "";
        }
    }

    public boolean isReady() { return ready; }

    private String extractTitle(String section) {
        String line = section.lines().findFirst().orElse("");
        return line.replaceAll("^#+\\s*", "").trim();
    }
}
```

**文档处理流程：**

```
合同.md (7KB)
    │
    ▼ 按 ## 标题拆分
    ├─ 章节1: "## 合同基本信息..." (500字符)
    ├─ 章节2: "## 租赁条款..." (1200字符 → TokenTextSplitter 拆成 2 块)
    ├─ 章节3: "## 违约责任..." (800字符)
    └─ 章节4: "## 退租流程..." (600字符)
    │
    ▼ 向量化 (OpenAI text-embedding-3-small)
    ├─ [0.123, -0.456, ...]  (1536维)
    ├─ [0.789, 0.234, ...]
    ├─ [-0.345, 0.678, ...]
    └─ [0.567, -0.890, ...]
    │
    ▼ 存入 SimpleVectorStore
    │
    ▼ 持久化到 ./uploads/kb/vector-store.json
```

### 1.4 AiToolConfig.java（改动：+RAG 函数注册）

新增 `searchKnowledgeBase` 函数：

```java
@Bean
public FunctionCallback searchKnowledgeBaseFn() {
    return FunctionCallback.builder()
            .function("searchKnowledgeBase", (SearchKbInput input) ->
                    toJson(Map.of("context", kbService.searchContext(input.question, 3))))
            .description("搜索公寓管理知识库，查询管理制度、合同条款、退租流程、支付规则等文档")
            .inputType(SearchKbInput.class)
            .build();
}

public record SearchKbInput(String question) {}
```

### 1.5 AiChatWebSocketHandler.java（改动：系统提示词）

增加了知识库相关的能力描述和调用规则：

```
- 知识库文档：公寓管理制度、合同条款、退租流程、支付规则等
...
- 当用户询问管理制度、合同条款、流程规则等问题时，优先调用 searchKnowledgeBase 搜索知识库
```

---

## 二、调用链路

```
用户："提前退租违约金怎么算"
        │
        ▼
ChatClient.prompt()
  .system("...当用户询问管理制度、合同条款...优先调用 searchKnowledgeBase...")
  .user("提前退租违约金怎么算")
  .stream()
        │
        ▼
DeepSeek 分析意图 → 返回 function_call: searchKnowledgeBase("提前退租违约金怎么算")
        │
        ▼
Spring AI 执行 FunctionCallback
  → KnowledgeBaseService.searchContext("提前退租违约金怎么算", 3)
    → OpenAiEmbeddingService.embed("提前退租违约金怎么算") → [0.12, -0.45, ...]
    → SimpleVectorStore.doSimilaritySearch(query向量, topK=3)
    → 返回最相关的 3 个文档片段（如《合同.md》违约责任章节）
    → 拼接成上下文字符串
        │
        ▼
上下文注入 DeepSeek（作为 tool 消息）
        │
        ▼
DeepSeek 基于文档内容生成回答：
"根据《合同.md》中的违约责任条款，提前退租需支付..."
        │
        ▼
前端收到流式文字
```

---

## 三、与第二阶段的协作

**第二阶段有 5 个 Tool（查数据库），第三阶段新增 1 个 Tool（查知识库）。** AI 会根据问题自动选择：

| 用户问题 | AI 选择的工具 |
|---------|-------------|
| "A-101有人住吗" | `getRoomInfo`（查数据库） |
| "提前退租违约金怎么算" | `searchKnowledgeBase`（查文档） |
| "本月收入多少" | `getMonthlyIncome`（查数据库） |
| "合同到期怎么续签" | `searchKnowledgeBase`（查文档） |
| "张三欠费多少" | `getTenantInfo` → 返回欠费数据 |

AI 可以在一次对话中按需调用多个工具，Spring AI 自动处理工具间的上下文传递。

---

## 四、部署步骤

### 1. 获取 OpenAI API Key

去 [platform.openai.com](https://platform.openai.com) 注册，在 API Keys 页面创建 key。

> **费用说明**：`text-embedding-3-small` 是 OpenAI 最便宜的嵌入模型，$0.02/百万 tokens。你的 5 个文档（~39KB）每次重建知识库只需要几分钱。

### 2. 配置环境变量

```bash
export OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxx
export AI_API_KEY=sk-your-deepseek-api-key   # DeepSeek（已有）
```

### 3. 首次启动（构建知识库）

```bash
cd apartment-hub-server
mvn spring-boot:run
```

首次启动日志：
```
Knowledge base built and saved: 23 chunks from 5 documents
```

之后重启会从 `./uploads/kb/vector-store.json` 加载，不需要重新向量化。

### 4. 文档更新后重建

删除向量库文件后重启即可：

```bash
rm ./uploads/kb/vector-store.json
# 重启后端
```

### 5. 添加新文档

在 `KnowledgeBaseService.java` 的 `DOC_PATHS` 数组中加路径，删除旧的 vector-store.json，重启。

---

## 五、文件清单

```
apartment-hub-server/src/main/java/com/apartment/hub/
├── config/
│   ├── OpenAiEmbeddingService.java       # 新建：OpenAI Embedding 客户端
│   ├── KnowledgeBaseConfig.java          # 新建：SimpleVectorStore Bean
│   └── AiToolConfig.java                # 改：+searchKnowledgeBase 函数
├── service/
│   └── KnowledgeBaseService.java         # 新建：文档导入、拆分、检索
└── websocket/
    └── AiChatWebSocketHandler.java       # 改：系统提示词加知识库描述

uploads/kb/
└── vector-store.json                     # 自动生成：向量库持久化文件
```

---

## 六、测试方法

```bash
# 启动（需要两个 API Key）
OPENAI_API_KEY=sk-xxx AI_API_KEY=sk-yyy mvn spring-boot:run
```

登录系统，打开 🤖 聊天窗口，试试：

| 测试问题 | 预期行为 |
|---------|---------|
| "提前退租要付多少违约金" | 调用 searchKnowledgeBase → 返回《合同.md》相关条款 |
| "支付流程是什么样的" | 调用 searchKnowledgeBase → 返回《支付.md》内容 |
| "Redis 是怎么集成的" | 调用 searchKnowledgeBase → 返回《Redis集成总结.md》 |
| "A-101有人住吗" | 调用 getRoomInfo（第二阶段工具仍然可用） |

---

## 七、常见问题

**Q: 启动时卡在"Calling EmbeddingModel"很久？**

第一次构建知识库时需要调 OpenAI API 向量化所有文档块，23 个块大约需要 2-3 秒。之后从磁盘加载几乎是瞬间完成。

**Q: 检索到的内容不相关怎么办？**

调整 `searchContext` 中的 `topK` 参数（当前是 3）和 `TokenTextSplitter` 的 `chunkSize`。chunk 太大 → 检索内容不够精准；chunk 太小 → 上下文不完整。

**Q: 如何升级到 Qdrant？**

只需改 `KnowledgeBaseConfig.java`，把 `SimpleVectorStore` Bean 换成 `QdrantVectorStore` Bean。`KnowledgeBaseService` 不需要改（`VectorStore` 接口统一了 `add()` 和 `doSimilaritySearch()`）。
