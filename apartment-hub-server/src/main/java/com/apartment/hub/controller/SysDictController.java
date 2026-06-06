package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.Result;
import com.apartment.hub.entity.SysDict;
import com.apartment.hub.service.SysDictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sys/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    @GetMapping("/list")
    public Result<List<SysDict>> list(@RequestParam(required = false) String dictType) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dictType != null, SysDict::getDictType, dictType)
                .orderByAsc(SysDict::getSortOrder);
        return Result.success(sysDictService.list(wrapper));
    }

    @OperationLog(module = "Dictionary Management", operation = "Create Dict")
    @PostMapping
    public Result<Boolean> create(@RequestBody SysDict dict) {
        return Result.success(sysDictService.save(dict));
    }

    @OperationLog(module = "Dictionary Management", operation = "Update Dict")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        return Result.success(sysDictService.updateById(dict));
    }

    @OperationLog(module = "Dictionary Management", operation = "Delete Dict")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(sysDictService.removeById(id));
    }
}
