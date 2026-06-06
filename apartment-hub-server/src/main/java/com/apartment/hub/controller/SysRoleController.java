package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.RoleDTO;
import com.apartment.hub.entity.SysRole;
import com.apartment.hub.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sys/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/list")
    public Result<PageResult<SysRole>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(PageResult.from(sysRoleService.page(new Page<>(current, size))));
    }

    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        return Result.success(sysRoleService.list());
    }

    @OperationLog(module = "Role Management", operation = "Create Role")
    @PostMapping
    public Result<Boolean> create(@Valid @RequestBody RoleDTO dto) {
        SysRole role = new SysRole();
        org.springframework.beans.BeanUtils.copyProperties(dto, role);
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        sysRoleService.save(role);
        if (dto.getPermissionIds() != null) {
            sysRoleService.assignPermissions(role.getId(), dto.getPermissionIds());
        }
        return Result.success();
    }

    @OperationLog(module = "Role Management", operation = "Update Role")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        SysRole role = sysRoleService.getById(id);
        if (role == null) return Result.fail("Role not found");
        org.springframework.beans.BeanUtils.copyProperties(dto, role);
        sysRoleService.updateById(role);
        if (dto.getPermissionIds() != null) {
            sysRoleService.assignPermissions(id, dto.getPermissionIds());
        }
        return Result.success();
    }

    @OperationLog(module = "Role Management", operation = "Delete Role")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        return Result.success(sysRoleService.getRolePermissionIds(id));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(sysRoleService.removeById(id));
    }
}
