package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.TenantDTO;
import com.apartment.hub.entity.Contract;
import com.apartment.hub.entity.Tenant;
import com.apartment.hub.service.ContractService;
import com.apartment.hub.service.TenantService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final ContractService contractService;

    @GetMapping("/list")
    public Result<PageResult<Tenant>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String idCard,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Tenant::getName, name)
                .like(StringUtils.hasText(phone), Tenant::getPhone, phone)
                .like(StringUtils.hasText(idCard), Tenant::getIdCard, idCard)
                .orderByDesc(Tenant::getCreateTime);
        return Result.success(PageResult.from(tenantService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/{id}")
    public Result<Tenant> getById(@PathVariable Long id) {
        return Result.success(tenantService.getById(id));
    }

    @GetMapping("/check-phone")
    public Result<Boolean> checkPhone(@RequestParam String phone, @RequestParam(required = false) Long excludeId) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getPhone, phone);
        if (excludeId != null) wrapper.ne(Tenant::getId, excludeId);
        return Result.success(tenantService.count(wrapper) == 0);
    }

    @GetMapping("/check-idcard")
    public Result<Boolean> checkIdCard(@RequestParam String idCard, @RequestParam(required = false) Long excludeId) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getIdCard, idCard);
        if (excludeId != null) wrapper.ne(Tenant::getId, excludeId);
        return Result.success(tenantService.count(wrapper) == 0);
    }

    @OperationLog(module = "Tenant Management", operation = "Create Tenant")
    @PostMapping
    public Result<Boolean> create(@Valid @RequestBody TenantDTO dto) {
        Tenant tenant = new Tenant();
        org.springframework.beans.BeanUtils.copyProperties(dto, tenant);
        return Result.success(tenantService.save(tenant));
    }

    @OperationLog(module = "Tenant Management", operation = "Update Tenant")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody TenantDTO dto) {
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) return Result.fail("Tenant not found");
        org.springframework.beans.BeanUtils.copyProperties(dto, tenant);
        return Result.success(tenantService.updateById(tenant));
    }

    @OperationLog(module = "Tenant Management", operation = "Delete Tenant")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (contractService.count(new LambdaQueryWrapper<Contract>().eq(Contract::getTenantId, id)) > 0) {
            return Result.fail("Cannot delete tenant with existing contracts");
        }
        return Result.success(tenantService.removeById(id));
    }
}
