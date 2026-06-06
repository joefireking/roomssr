package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.ApartmentDTO;
import com.apartment.hub.entity.Apartment;
import com.apartment.hub.entity.Building;
import com.apartment.hub.service.ApartmentService;
import com.apartment.hub.service.BuildingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final BuildingService buildingService;

    @GetMapping("/list")
    public Result<PageResult<Apartment>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Apartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Apartment::getName, name)
                .eq(StringUtils.hasText(city), Apartment::getCity, city)
                .eq(status != null, Apartment::getStatus, status)
                .orderByDesc(Apartment::getCreateTime);
        return Result.success(PageResult.from(apartmentService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/{id}")
    public Result<Apartment> getById(@PathVariable Long id) {
        return Result.success(apartmentService.getById(id));
    }

    @OperationLog(module = "Apartment Management", operation = "Create Apartment")
    @PostMapping
    public Result<Boolean> create(@Valid @RequestBody ApartmentDTO dto) {
        Apartment apartment = new Apartment();
        org.springframework.beans.BeanUtils.copyProperties(dto, apartment);
        apartment.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return Result.success(apartmentService.save(apartment));
    }

    @OperationLog(module = "Apartment Management", operation = "Update Apartment")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody ApartmentDTO dto) {
        Apartment apartment = apartmentService.getById(id);
        if (apartment == null) return Result.fail("Apartment not found");
        org.springframework.beans.BeanUtils.copyProperties(dto, apartment);
        return Result.success(apartmentService.updateById(apartment));
    }

    @OperationLog(module = "Apartment Management", operation = "Delete Apartment")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (buildingService.count(new LambdaQueryWrapper<Building>().eq(Building::getApartmentId, id)) > 0) {
            return Result.fail("Cannot delete apartment with existing buildings");
        }
        return Result.success(apartmentService.removeById(id));
    }
}
