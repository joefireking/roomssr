package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.Result;
import com.apartment.hub.entity.SysPermission;
import com.apartment.hub.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.success(sysPermissionService.getMenuTree());
    }

    @GetMapping("/list")
    public Result<List<SysPermission>> list() {
        return Result.success(sysPermissionService.list(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSortOrder)));
    }

    @OperationLog(module = "Permission Management", operation = "Create Permission")
    @PostMapping
    public Result<Boolean> create(@RequestBody SysPermission permission) {
        return Result.success(sysPermissionService.save(permission));
    }

    @OperationLog(module = "Permission Management", operation = "Update Permission")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setId(id);
        return Result.success(sysPermissionService.updateById(permission));
    }

    @OperationLog(module = "Permission Management", operation = "Delete Permission")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(sysPermissionService.removeById(id));
    }
}
