package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.entity.Room;
import com.apartment.hub.entity.RoomType;
import com.apartment.hub.service.RoomService;
import com.apartment.hub.service.RoomTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final RoomService roomService;

    @Cacheable(value = "roomTypes", key = "'list:' + #current + ':' + #size", unless = "#result == null")
    @GetMapping("/list")
    public Result<PageResult<RoomType>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(PageResult.from(roomTypeService.page(new Page<>(current, size))));
    }

    @Cacheable(value = "roomTypes", key = "'all'", unless = "#result == null")
    @GetMapping("/all")
    public Result<List<RoomType>> all() {
        return Result.success(roomTypeService.list());
    }

    @GetMapping("/{id}")
    public Result<RoomType> getById(@PathVariable Long id) {
        return Result.success(roomTypeService.getById(id));
    }

    @CacheEvict(value = "roomTypes", allEntries = true)
    @OperationLog(module = "Room Type Management", operation = "Create Room Type")
    @PostMapping
    public Result<Boolean> create(@RequestBody RoomType roomType) {
        return Result.success(roomTypeService.save(roomType));
    }

    @CacheEvict(value = "roomTypes", allEntries = true)
    @OperationLog(module = "Room Type Management", operation = "Update Room Type")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody RoomType roomType) {
        roomType.setId(id);
        return Result.success(roomTypeService.updateById(roomType));
    }

    @CacheEvict(value = "roomTypes", allEntries = true)
    @OperationLog(module = "Room Type Management", operation = "Delete Room Type")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (roomService.count(new LambdaQueryWrapper<Room>().eq(Room::getRoomTypeId, id)) > 0) {
            return Result.fail("Cannot delete room type referenced by existing rooms");
        }
        return Result.success(roomTypeService.removeById(id));
    }
}
