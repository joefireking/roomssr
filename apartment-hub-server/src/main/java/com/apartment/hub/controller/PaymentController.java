package com.apartment.hub.controller;

import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.entity.Payment;
import com.apartment.hub.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/list")
    public Result<PageResult<Payment>> list(
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(billId != null, Payment::getBillId, billId)
                .eq(tenantId != null, Payment::getTenantId, tenantId)
                .orderByDesc(Payment::getCreateTime);
        return Result.success(PageResult.from(paymentService.page(new Page<>(current, size), wrapper)));
    }
}
