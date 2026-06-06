package com.apartment.hub.controller;

import com.apartment.hub.common.Result;
import com.apartment.hub.entity.Bill;
import com.apartment.hub.enums.BillStatus;
import com.apartment.hub.service.BillService;
import com.apartment.hub.service.RoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final RoomService roomService;
    private final BillService billService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("roomStatus", roomService.countByStatus());
        data.put("totalPaid", billService.getTotalPaidAmount());
        data.put("totalPending", billService.getTotalPendingAmount());
        data.put("totalOverdue", billService.getTotalOverdueAmount());
        data.put("totalRooms", roomService.count());
        data.put("rentedRooms", roomService.lambdaQuery().eq(
                com.apartment.hub.entity.Room::getStatus, com.apartment.hub.enums.RoomStatus.RENTED).count());
        return Result.success(data);
    }

    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        return Result.success(billService.revenueByMonth(startMonth, endMonth));
    }

    @GetMapping("/overdue-top")
    public Result<List<Bill>> overdueTop(@RequestParam(defaultValue = "10") int limit) {
        Page<Bill> page = new Page<>(1, Math.min(limit, 100));
        return Result.success(billService.page(page, new LambdaQueryWrapper<Bill>()
                .eq(Bill::getStatus, BillStatus.OVERDUE)
                .orderByDesc(Bill::getAmount)).getRecords());
    }
}
