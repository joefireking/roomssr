# AI 第二阶段：Tool Calling（AI 调用真实业务数据）

## 概述

第一阶段 AI 只能基于通用知识对话，回答不了"数据库里 A-101 房间到底有没有人住"。第二阶段引入 **Tool Calling**，让 AI 能自动调用后端的 Service 方法查询真实数据，再整合成自然语言回复。

## 核心原理

```
用户："A-101房间有人住吗？"
        │
        ▼
ChatClient.prompt("A-101房间有人住吗？")
  .functions(getRoomInfo, getOverdueBills, ...)    ← 告诉AI有哪些工具可用
  .stream()
        │
        ▼
DeepSeek 分析意图 → 返回 function_call: getRoomInfo("A-101")
        │
        ▼
Spring AI 自动执行 FunctionCallback
  → AiToolService.getRoomInfo("A-101")
  → MyBatis-Plus 查数据库
  → 返回 {status:"Rented", tenantName:"张三", ...}
        │
        ▼
结果自动注入回 DeepSeek → 生成："A-101目前有人住，租客是张三..."
        │
        ▼
前端收到流式文字
```

**关键点：整个 Tool Calling 的循环由 Spring AI 自动完成，不需要你写 if-else 判断用户意图。**

## 技术栈

| 层级 | 技术 |
|------|------|
| Function Calling | Spring AI `FunctionCallback` + `ChatClient` |
| 工具执行 | `AiToolService` → MyBatis-Plus `LambdaQueryWrapper` |
| 注册方式 | `@Configuration` + `@Bean` 返回 `FunctionCallback` |
| 大模型 | DeepSeek（支持 OpenAI 兼容的 function calling） |
| 版本适配 | Spring AI 1.0.0-M5（API 与 GA 版本不同，详见下文） |

---

## 一、与第一阶段的架构对比

```
第一阶段（纯对话）：
  ChatModel.stream(prompt) → DeepSeek API → 流式回复
  AI 只用通用知识，不碰数据库

第二阶段（Tool Calling）：
  ChatClient.prompt().functions(...).stream().chatResponse()
  → DeepSeek 先判断是否需要调用工具
  → 如果需要，返回 function_call
  → Spring AI 执行 FunctionCallback（查数据库）
  → 结果注入回 DeepSeek
  → 继续流式输出最终文字
```

**改动量很小** —— 只改了 `AiChatWebSocketHandler`（4 行改 3 行），新增了 `AiToolService` 和 `AiToolConfig` 两个文件。

---

## 二、后端代码

### 2.1 AiToolService.java（新建，核心）

这个类包含 5 个查询方法，每个方法直接调用项目现有的 Service 层查数据库。

```java
package com.apartment.hub.tool;

import com.apartment.hub.entity.*;
import com.apartment.hub.enums.BillStatus;
import com.apartment.hub.enums.ContractStatus;
import com.apartment.hub.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolService {

    private final RoomService roomService;
    private final ContractService contractService;
    private final BillService billService;
    private final TenantService tenantService;
    private final BuildingService buildingService;

    // ==================== 工具1：按房间号查询 ====================
    public Map<String, Object> getRoomInfo(String roomNumber) {
        log.info("AI Tool: getRoomInfo({})", roomNumber);

        // Step 1: 查房间
        Room room = roomService.getOne(
                new LambdaQueryWrapper<Room>().eq(Room::getRoomNumber, roomNumber));
        if (room == null) {
            return Map.of("found", false, "message", "未找到房间号 " + roomNumber);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("roomNumber", room.getRoomNumber());
        result.put("floor", room.getFloor());
        result.put("rentPrice", room.getRentPrice());
        result.put("status", room.getStatus().getDescription());   // "Vacant" / "Rented"

        // Step 2: 查楼栋
        if (room.getBuildingId() != null) {
            Building building = buildingService.getById(room.getBuildingId());
            if (building != null) result.put("building", building.getName());
        }

        // Step 3: 查当前有效合同
        Contract activeContract = contractService.getOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getRoomId, room.getId())
                        .eq(Contract::getStatus, ContractStatus.ACTIVE));
        if (activeContract != null) {
            result.put("contractNo", activeContract.getContractNo());
            result.put("contractStart", activeContract.getStartDate().toString());
            result.put("contractEnd", activeContract.getEndDate().toString());
            result.put("depositAmount", activeContract.getDepositAmount());

            // Step 4: 查租客
            Tenant tenant = tenantService.getById(activeContract.getTenantId());
            if (tenant != null) {
                result.put("tenantName", tenant.getName());
                result.put("tenantPhone", tenant.getPhone());
            }
        }

        return result;
    }

    // ==================== 工具2：查询逾期账单 ====================
    public Map<String, Object> getOverdueBills() {
        log.info("AI Tool: getOverdueBills()");

        List<Bill> overdueBills = billService.list(
                new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, BillStatus.OVERDUE));
        if (overdueBills.isEmpty()) {
            return Map.of("count", 0, "message", "当前没有逾期账单");
        }

        BigDecimal totalOverdue = overdueBills.stream()
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (Bill bill : overdueBills) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("billNo", bill.getBillNo());
            item.put("amount", bill.getAmount());
            item.put("billingMonth", bill.getBillingMonth());
            item.put("dueDate", bill.getDueDate().toString());

            Tenant tenant = tenantService.getById(bill.getTenantId());
            if (tenant != null) {
                item.put("tenantName", tenant.getName());
                item.put("tenantPhone", tenant.getPhone());
            }

            Room room = roomService.getById(bill.getRoomId());
            if (room != null) item.put("roomNumber", room.getRoomNumber());

            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", detailList.size());
        result.put("totalAmount", totalOverdue);
        result.put("bills", detailList);
        return result;
    }

    // ==================== 工具3：查询即将到期的合同 ====================
    public Map<String, Object> getExpiringContracts(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(withinDays);

        List<Contract> expiring = contractService.list(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, ContractStatus.ACTIVE)
                        .between(Contract::getEndDate, today, deadline)
                        .orderByAsc(Contract::getEndDate));

        if (expiring.isEmpty()) {
            return Map.of("count", 0, "message",
                    "未来" + withinDays + "天内没有即将到期的合同");
        }

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (Contract c : expiring) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("contractNo", c.getContractNo());
            item.put("endDate", c.getEndDate().toString());
            item.put("rentAmount", c.getRentAmount());
            item.put("daysRemaining", LocalDate.now().until(c.getEndDate()).getDays());

            Tenant tenant = tenantService.getById(c.getTenantId());
            if (tenant != null) {
                item.put("tenantName", tenant.getName());
                item.put("tenantPhone", tenant.getPhone());
            }

            Room room = roomService.getById(c.getRoomId());
            if (room != null) item.put("roomNumber", room.getRoomNumber());

            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", detailList.size());
        result.put("withinDays", withinDays);
        result.put("contracts", detailList);
        return result;
    }

    // ==================== 工具4：本月收入统计 ====================
    public Map<String, Object> getMonthlyIncome() {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Map<String, Object>> revenueList = billService.revenueByMonth(currentMonth, currentMonth);

        BigDecimal totalPaid = billService.getTotalPaidAmount();
        BigDecimal totalPending = billService.getTotalPendingAmount();
        BigDecimal totalOverdue = billService.getTotalOverdueAmount();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentMonth", currentMonth);
        result.put("monthlyPaid", revenueList.isEmpty() ? BigDecimal.ZERO
                : revenueList.get(0).getOrDefault("amount", BigDecimal.ZERO));
        result.put("totalPaidAllTime", totalPaid);
        result.put("totalPending", totalPending);
        result.put("totalOverdue", totalOverdue);

        // 房间统计：空置/已租/维修/预留
        List<Map<String, Object>> statusCount = roomService.countByStatus();
        Map<String, Long> roomStats = new LinkedHashMap<>();
        for (Map<String, Object> sc : statusCount) {
            String statusName = String.valueOf(sc.get("status"));
            Number count = (Number) sc.get("count");
            if (statusName.equals("0")) roomStats.put("vacant", count.longValue());
            else if (statusName.equals("1")) roomStats.put("rented", count.longValue());
            else if (statusName.equals("2")) roomStats.put("maintenance", count.longValue());
            else if (statusName.equals("3")) roomStats.put("reserved", count.longValue());
        }
        result.put("roomStats", roomStats);

        return result;
    }

    // ==================== 工具5：按姓名查租客 ====================
    public Map<String, Object> getTenantInfo(String name) {
        List<Tenant> tenants = tenantService.list(
                new LambdaQueryWrapper<Tenant>().like(Tenant::getName, name));
        if (tenants.isEmpty()) {
            return Map.of("found", false, "message", "未找到姓名包含 \"" + name + "\" 的租客");
        }

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (Tenant t : tenants) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", t.getName());
            item.put("phone", t.getPhone());
            item.put("gender", t.getGender() == 1 ? "男" : "女");
            item.put("tag", t.getTag());

            // 查当前合同和房间
            Contract activeContract = contractService.getOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getTenantId, t.getId())
                            .eq(Contract::getStatus, ContractStatus.ACTIVE));
            if (activeContract != null) {
                item.put("contractNo", activeContract.getContractNo());
                item.put("contractStart", activeContract.getStartDate().toString());
                item.put("contractEnd", activeContract.getEndDate().toString());
                Room room = roomService.getById(activeContract.getRoomId());
                if (room != null) item.put("roomNumber", room.getRoomNumber());
            }

            // 查欠费
            List<Bill> overdueBills = billService.list(
                    new LambdaQueryWrapper<Bill>()
                            .eq(Bill::getTenantId, t.getId())
                            .eq(Bill::getStatus, BillStatus.OVERDUE));
            if (!overdueBills.isEmpty()) {
                BigDecimal totalOwed = overdueBills.stream()
                        .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                item.put("overdueAmount", totalOwed);
            }

            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("count", detailList.size());
        result.put("tenants", detailList);
        return result;
    }
}
```

**设计原则：**
- 每个方法返回 `Map<String, Object>`，由 Jackson 序列化为 JSON 后传给 AI
- 包含 `found` / `count` 等字段，方便 AI 判断"有没有数据"
- 没有数据的场景也返回明确 message，AI 会如实告知用户
- 使用 `@Slf4j` 打印日志，方便排查 AI 实际调用了哪个工具

### 2.2 AiToolConfig.java（新建）

把 `AiToolService` 里的查询方法包装成 Spring AI 的 `FunctionCallback` Bean。

```java
package com.apartment.hub.config;

import com.apartment.hub.tool.AiToolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AiToolConfig {

    private final AiToolService toolService;
    private final ObjectMapper objectMapper;

    @Bean
    public FunctionCallback getRoomInfoFn() {
        return FunctionCallback.builder()
                .function("getRoomInfo", (GetRoomInfoInput input) ->
                        toJson(toolService.getRoomInfo(input.roomNumber)))
                .description("按房间号查询房间的入住状态、租客信息、合同信息。参数 roomNumber 格式如 A-102")
                .inputType(GetRoomInfoInput.class)
                .build();
    }

    @Bean
    public FunctionCallback getOverdueBillsFn() {
        return FunctionCallback.builder()
                .function("getOverdueBills", (NoArgsInput input) ->
                        toJson(toolService.getOverdueBills()))
                .description("查询当前所有逾期未缴的账单，包含租客姓名、房间号、欠费金额")
                .inputType(NoArgsInput.class)
                .build();
    }

    @Bean
    public FunctionCallback getExpiringContractsFn() {
        return FunctionCallback.builder()
                .function("getExpiringContracts", (GetExpiringContractsInput input) ->
                        toJson(toolService.getExpiringContracts(
                                input.withinDays > 0 ? input.withinDays : 30)))
                .description("查询未来N天内即将到期的合同列表。参数 withinDays 为天数，默认30天")
                .inputType(GetExpiringContractsInput.class)
                .build();
    }

    @Bean
    public FunctionCallback getMonthlyIncomeFn() {
        return FunctionCallback.builder()
                .function("getMonthlyIncome", (NoArgsInput input) ->
                        toJson(toolService.getMonthlyIncome()))
                .description("查询本月收入统计和房间入住概况，包含已收/待收/逾期金额")
                .inputType(NoArgsInput.class)
                .build();
    }

    @Bean
    public FunctionCallback getTenantInfoFn() {
        return FunctionCallback.builder()
                .function("getTenantInfo", (GetTenantInfoInput input) ->
                        toJson(toolService.getTenantInfo(input.name)))
                .description("按姓名模糊查询租客信息，包含合同、房间、欠费情况。参数 name 为租客姓名")
                .inputType(GetTenantInfoInput.class)
                .build();
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    // Record 类用于 Spring AI 自动生成 JSON Schema
    public record GetRoomInfoInput(String roomNumber) {}
    public record GetExpiringContractsInput(int withinDays) {}
    public record GetTenantInfoInput(String name) {}
    public record NoArgsInput() {}
}
```

**关键点：**
- `FunctionCallback.builder()` → `.function(name, lambda)` → `.description(desc)` → `.inputType(Class)` → `.build()`
- 每个 Bean 的名字就是函数名，注册后 Spring AI 会自动发现
- `inputType` 用 Record 类定义，Spring AI 会自动根据字段类型生成 JSON Schema 发给 DeepSeek
- 无参数工具用空 Record `NoArgsInput`，AI 调用时不需要传参
- 返回值必须序列化为 JSON 字符串（`toJson()`），AI 只理解 JSON
- 查询结果以结构化 `Map` 返回，方便 AI 提取关键字段组织语言

### 2.3 AiChatWebSocketHandler.java（改动 3 行）

先从 `ChatModel` 改为 `ChatClient`，再注入工具函数。

```java
// ========== 第一阶段的构造函数 ==========
public AiChatWebSocketHandler(ChatModel chatModel, ObjectMapper objectMapper) {
    this.chatModel = chatModel;   // 只注入聊天模型
    this.objectMapper = objectMapper;
}

// ========== 第二阶段的构造函数 ==========
public AiChatWebSocketHandler(ChatModel chatModel, ObjectMapper objectMapper,
                               List<FunctionCallback> functionCallbacks) {
    this.objectMapper = objectMapper;
    this.chatClient = ChatClient.builder(chatModel)          // ChatModel → ChatClient
            .defaultFunctions(functionCallbacks.toArray(new FunctionCallback[0]))  // 注入工具
            .build();
}
```

```java
// ========== 第一阶段的消息处理 ==========
Prompt prompt = new Prompt(
        new SystemMessage(SYSTEM_PROMPT),
        new UserMessage(userMessage)
);
chatModel.stream(prompt)          // ChatModel.stream()
        .doOnNext(...)
        ...

// ========== 第二阶段的消息处理 ==========
chatClient.prompt()               // ChatClient.prompt()
        .system(SYSTEM_PROMPT)    // 链式 API：.system() + .user()
        .user(userMessage)
        .stream()                 // 工具调用在流式过程中自动完成
        .chatResponse()           // 返回的 Flux 只包含最终文字，不包含工具调用过程
        .doOnNext(...)
        ...
```

**为什么 ChantClient 会自动处理 Tool Calling？**

`ChatClient` 内部会：
1. 把工具函数列表（functionCallbacks）序列化为 JSON Schema，随请求发给 DeepSeek
2. DeepSeek 返回 `function_call` 时，匹配对应的 `FunctionCallback` 并执行
3. 把执行结果作为 `tool` 消息发回 DeepSeek
4. DeepSeek 基于工具结果生成最终文字
5. 最终文字通过 `Flux<ChatResponse>` 流式返回

**所以对前端来说协议完全不变** —— 仍然收到 `start → token → token → ... → done`。

### 2.4 系统提示词变更

```java
// ========== 第一阶段 ==========
private static final String SYSTEM_PROMPT = """
        你是公寓管理系统的 AI 助理，名叫"小安"。你的职责是帮助用户管理公寓业务，包括：
        - 查询房间入住状态
        - 查询合同信息
        - 查询账单和缴费情况
        回复控制在200字以内
        """;

// ========== 第二阶段 ==========
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
        """;
```

**关键差异：第二阶段必须明确告诉 AI"你有工具、什么时候用工具、不要编造"。**

---

## 三、Spring AI 1.0.0-M5 API 适配说明

Spring AI 1.0.0-M5 的 API 与 GA 版本（1.0.0+）不同，以下是 M5 的实际 API：

### FunctionCallback.Builder

```java
// M5 的正确写法（不是 .name()，是 .function(name, fn)）
FunctionCallback.builder()
    .function("functionName", (InputType input) -> { ... })  // 第一个参数是函数名
    .description("描述")                                      // CommonCallbackInvokingSpec
    .inputType(InputType.class)                               // 输入类型，用于生成 JSON Schema
    .build();

// ❌ M5 不支持的写法（这是 GA 的 API）
FunctionCallback.builder()
    .name("functionName")         // M5 没有 .name()
    .function((InputType input) -> { ... })
    ...
```

### ChatClient.Builder

```java
// M5 的正确写法
ChatClient.builder(chatModel)
    .defaultFunctions(FunctionCallback... callbacks)  // 接受 varargs，不是 List
    .build();

// 如果注入的是 List<FunctionCallback>，需要转换：
chatClient = ChatClient.builder(chatModel)
    .defaultFunctions(functionCallbacks.toArray(new FunctionCallback[0]))
    .build();
```

### ChatClient.ChatClientRequestSpec（流式调用）

```java
// M5 的链式 API
chatClient.prompt()
    .system(SYSTEM_PROMPT)     // 系统提示词
    .user(userMessage)         // 用户消息
    .stream()                  // 返回 StreamResponseSpec
    .chatResponse()            // 返回 Flux<ChatResponse>
    .doOnNext(...)
    .blockLast();
```

---

## 四、5 个工具函数一览

| 函数名 | 参数 | AI 何时调用 | 查询的表 | 返回数据 |
|--------|------|------------|---------|---------|
| `getRoomInfo` | roomNumber: String | "A-101有人住吗" | room, contract, tenant, building | 房间状态、租客名、合同期、租金 |
| `getOverdueBills` | 无 | "谁欠费了"、"逾期账单有哪些" | bill, tenant, room | 逾期账单列表、欠费总额、房间号 |
| `getExpiringContracts` | withinDays: int | "下个月到期的合同有哪些" | contract, tenant, room | 到期合同列表、剩余天数 |
| `getMonthlyIncome` | 无 | "本月收入多少" | bill, room | 已收/待收/逾期金额、房间入住统计 |
| `getTenantInfo` | name: String | "张三的租客信息" | tenant, contract, room, bill | 租客资料、当前合同、欠费情况 |

---

## 五、与第一阶段的前后端差异

| 方面 | 第一阶段 | 第二阶段 |
|------|---------|---------|
| 后端改in' | 3 个新文件 | +2 个新文件，改 1 个 |
| 前端改动 | 1 个新组件，2 个文件改动 | **无**（只更新了建议问题列表） |
| 消息协议 | `token` / `done` / `error` | **不变** |
| 数据库访问 | 无 | AI 通过 Tool 自动访问 |
| AI 回答质量 | 通用知识 | 真实数据 |

---

## 六、文件清单

```
apartment-hub-server/src/main/java/com/apartment/hub/
├── config/
│   ├── AiToolConfig.java              # 新建：注册 5 个 FunctionCallback Bean
│   └── SecurityConfig.java           # 未变
├── tool/
│   └── AiToolService.java            # 新建：5 个数据查询方法
└── websocket/
    └── AiChatWebSocketHandler.java    # 改：ChatModel → ChatClient + 注入工具

apartment-hub-web/src/
└── components/
    └── AiChat.vue                     # 改：更新建议问题列表
```

---

## 七、测试方法

启动后端和前端后，登录系统，打开 🤖 聊天窗口，尝试以下问题：

| 测试问题 | 预期 AI 行为 |
|---------|-------------|
| `A-101房间有人住吗` | 调用 getRoomInfo("A-101") → 返回真实数据或"未找到" |
| `有哪些逾期未缴的账单` | 调用 getOverdueBills() → 列出欠费清单或"当前没有" |
| `未来60天内到期的合同有哪些` | 调用 getExpiringContracts(60) → 列出到期合同 |
| `本月租金收入多少` | 调用 getMonthlyIncome() → 返回收入统计数据 |
| `帮我查一下张三的租客信息` | 调用 getTenantInfo("张三") → 返回租客详情 |

---

## 八、常见问题

**Q: AI 为什么没有调用工具？**

检查后端日志，看是否有 `AI Tool: getXxx()` 输出。如果没有：
- 系统提示词是否明确告诉 AI 要使用工具
- `FunctionCallback` 的 `description` 是否清晰描述了工具用途
- DeepSeek 的 function calling 能力是否正常

**Q: 工具返回了数据，但 AI 回复看起来还是编的？**

检查 `SystemMessage` 中的系统提示词，确保有"用户问数据相关的问题时，必须调用工具查询，不要编造答案"这条规则。

**Q: 怎么添加新的工具？**

三步：
1. 在 `AiToolService` 中添加查询方法
2. 在 `AiToolConfig` 中注册为 `FunctionCallback` Bean
3. 更新 `SYSTEM_PROMPT` 描述新工具的用途

Spring AI 的 `ChatClient` 会自动发现新的 `FunctionCallback` Bean（通过构造函数注入 `List<FunctionCallback>`）。
