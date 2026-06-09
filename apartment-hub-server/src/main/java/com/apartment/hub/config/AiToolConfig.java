package com.apartment.hub.config;

import com.apartment.hub.service.KnowledgeBaseService;
import com.apartment.hub.tool.AiToolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AiToolConfig {

    private final AiToolService toolService;
    private final KnowledgeBaseService kbService;
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

    @Bean
    public FunctionCallback searchKnowledgeBaseFn() {
        return FunctionCallback.builder()
                .function("searchKnowledgeBase", (SearchKbInput input) ->
                        toJson(Map.of("context", kbService.searchContext(input.question, 3))))
                .description("搜索公寓管理知识库，查询管理制度、合同条款、退租流程、支付规则等文档。参数 question 为要查询的问题")
                .inputType(SearchKbInput.class)
                .build();
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    // Input types for function schema generation
    public record GetRoomInfoInput(String roomNumber) {}
    public record GetExpiringContractsInput(int withinDays) {}
    public record GetTenantInfoInput(String name) {}
    public record SearchKbInput(String question) {}
    public record NoArgsInput() {}
}
