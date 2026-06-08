package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.BuildingDTO;
import com.apartment.hub.entity.Building;
import com.apartment.hub.entity.Room;
import com.apartment.hub.service.BuildingService;
import com.apartment.hub.service.RoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;
    private final RoomService roomService;

    @Cacheable(value = "buildings", key = "'list:' + #apartmentId + ':' + #name + ':' + #current + ':' + #size", unless = "#result == null")
    @GetMapping("/list")
    public Result<PageResult<Building>> list(
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(apartmentId != null, Building::getApartmentId, apartmentId)
                .like(name != null, Building::getName, name)
                .orderByDesc(Building::getCreateTime);
        return Result.success(PageResult.from(buildingService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/by-apartment/{apartmentId}")
    public Result<List<Building>> byApartment(@PathVariable Long apartmentId) {
        return Result.success(buildingService.list(new LambdaQueryWrapper<Building>()
                .eq(Building::getApartmentId, apartmentId)));
    }

    @GetMapping("/{id}")
    public Result<Building> getById(@PathVariable Long id) {
        return Result.success(buildingService.getById(id));
    }

    @CacheEvict(value = "buildings", allEntries = true)
    @OperationLog(module = "Building Management", operation = "Create Building")
    @PostMapping
    public Result<Boolean> create(@Valid @RequestBody BuildingDTO dto) {
        Building building = new Building();
        org.springframework.beans.BeanUtils.copyProperties(dto, building);
        building.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return Result.success(buildingService.save(building));
    }

    @CacheEvict(value = "buildings", allEntries = true)
    @OperationLog(module = "Building Management", operation = "Update Building")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody BuildingDTO dto) {
        Building building = buildingService.getById(id);
        if (building == null) return Result.fail("Building not found");
        org.springframework.beans.BeanUtils.copyProperties(dto, building);
        return Result.success(buildingService.updateById(building));
    }

    @CacheEvict(value = "buildings", allEntries = true)
    @OperationLog(module = "Building Management", operation = "Delete Building")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (roomService.count(new LambdaQueryWrapper<Room>().eq(Room::getBuildingId, id)) > 0) {
            return Result.fail("Cannot delete building with existing rooms");
        }
        return Result.success(buildingService.removeById(id));
    }
}
