package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.RepairOrderDTO;
import com.apartment.hub.entity.RepairOrder;
import com.apartment.hub.enums.RepairPriority;
import com.apartment.hub.enums.RepairStatus;
import com.apartment.hub.enums.RepairType;
import com.apartment.hub.service.RepairOrderService;
import com.apartment.hub.util.NoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repair-orders")
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    @GetMapping("/list")
    public Result<PageResult<RepairOrder>> list(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer priority,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(roomId != null, RepairOrder::getRoomId, roomId)
                .eq(type != null, RepairOrder::getType, type)
                .eq(status != null, RepairOrder::getStatus, status)
                .eq(priority != null, RepairOrder::getPriority, priority)
                .orderByDesc(RepairOrder::getCreateTime);
        return Result.success(PageResult.from(repairOrderService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/{id}")
    public Result<RepairOrder> getById(@PathVariable Long id) {
        return Result.success(repairOrderService.getById(id));
    }

    @OperationLog(module = "Repair Management", operation = "Create Repair Order")
    @PostMapping
    public Result<Boolean> create(@Valid @RequestBody RepairOrderDTO dto) {
        RepairOrder order = new RepairOrder();
        order.setOrderNo(NoGenerator.repairOrderNo());
        order.setRoomId(dto.getRoomId());
        order.setTenantId(dto.getTenantId());
        RepairType[] types = RepairType.values();
        if (dto.getType() < 0 || dto.getType() >= types.length) {
            return Result.fail("Invalid repair type: " + dto.getType());
        }
        order.setType(types[dto.getType()]);
        int priority = dto.getPriority() != null ? dto.getPriority() : 1;
        RepairPriority[] priorities = RepairPriority.values();
        if (priority < 0 || priority >= priorities.length) {
            return Result.fail("Invalid repair priority: " + priority);
        }
        order.setPriority(priorities[priority]);
        order.setDescription(dto.getDescription());
        order.setImages(dto.getImages());
        order.setStatus(RepairStatus.PENDING);
        return Result.success(repairOrderService.save(order));
    }

    @OperationLog(module = "Repair Management", operation = "Assign Repair Order")
    @PutMapping("/{id}/assign")
    public Result<Boolean> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        repairOrderService.assignOrder(id, assigneeId);
        return Result.success();
    }

    @OperationLog(module = "Repair Management", operation = "Start Repair Work")
    @PutMapping("/{id}/start")
    public Result<Boolean> start(@PathVariable Long id) {
        repairOrderService.startWork(id);
        return Result.success();
    }

    @OperationLog(module = "Repair Management", operation = "Complete Repair Work")
    @PutMapping("/{id}/complete")
    public Result<Boolean> complete(@PathVariable Long id, @RequestParam(required = false) String remark) {
        repairOrderService.completeWork(id, remark);
        return Result.success();
    }

    @OperationLog(module = "Repair Management", operation = "Verify Repair Order")
    @PutMapping("/{id}/verify")
    public Result<Boolean> verify(@PathVariable Long id) {
        repairOrderService.verifyOrder(id);
        return Result.success();
    }
}
