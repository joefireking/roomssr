package com.apartment.hub.controller;

import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.entity.OperationLog;
import com.apartment.hub.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/list")
    public Result<PageResult<OperationLog>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(module != null, OperationLog::getModule, module)
                .like(username != null, OperationLog::getUsername, username)
                .orderByDesc(OperationLog::getCreateTime);
        return Result.success(PageResult.from(operationLogService.page(new Page<>(current, size), wrapper)));
    }
}
