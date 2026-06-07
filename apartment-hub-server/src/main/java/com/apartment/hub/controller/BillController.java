package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.PaymentDTO;
import com.apartment.hub.entity.Bill;
import com.apartment.hub.security.LoginUser;
import com.apartment.hub.service.BillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @GetMapping("/list")
    public Result<PageResult<Bill>> list(
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer billType,
            @RequestParam(required = false) String billingMonth,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(contractId != null, Bill::getContractId, contractId)
                .eq(tenantId != null, Bill::getTenantId, tenantId)
                .eq(status != null, Bill::getStatus, status)
                .eq(billType != null, Bill::getBillType, billType)
                .eq(billingMonth != null, Bill::getBillingMonth, billingMonth)
                .orderByDesc(Bill::getCreateTime);
        return Result.success(PageResult.from(billService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/{id}")
    public Result<Bill> getById(@PathVariable Long id) {
        return Result.success(billService.getById(id));
    }

    @PreAuthorize("hasAuthority('bill:pay')")
    @OperationLog(module = "Bill Management", operation = "Pay Bill")
    @PostMapping("/{id}/pay")
    public Result<Boolean> pay(@PathVariable Long id, @Valid @RequestBody PaymentDTO dto,
                               @AuthenticationPrincipal LoginUser loginUser) {
        dto.setBillId(id);
        billService.payBill(dto, loginUser.getUserId());
        return Result.success();
    }

    @GetMapping("/overdue")
    public Result<PageResult<Bill>> overdue(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getStatus, 2).orderByDesc(Bill::getDueDate);
        return Result.success(PageResult.from(billService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPaid", billService.getTotalPaidAmount());
        stats.put("totalPending", billService.getTotalPendingAmount());
        stats.put("totalOverdue", billService.getTotalOverdueAmount());
        return Result.success(stats);
    }

    @OperationLog(module = "Bill Management", operation = "Check Overdue Bills")
    @PostMapping("/check-overdue")
    public Result<Boolean> checkOverdue() {
        billService.checkOverdue();
        return Result.success();
    }

    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        return Result.success(billService.revenueByMonth(startMonth, endMonth));
    }
}
