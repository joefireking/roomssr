package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.RoomDTO;
import com.apartment.hub.entity.Building;
import com.apartment.hub.entity.Contract;
import com.apartment.hub.entity.Room;
import com.apartment.hub.enums.RoomStatus;
import com.apartment.hub.service.BuildingService;
import com.apartment.hub.service.ContractService;
import com.apartment.hub.service.RoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final ContractService contractService;
    private final BuildingService buildingService;

    @GetMapping("/list")
    public Result<PageResult<Room>> list(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(buildingId != null, Room::getBuildingId, buildingId)
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .eq(status != null, Room::getStatus, status)
                .orderByDesc(Room::getCreateTime);
        return Result.success(PageResult.from(roomService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/by-building/{buildingId}")
    public Result<List<Room>> byBuilding(@PathVariable Long buildingId) {
        return Result.success(roomService.list(new LambdaQueryWrapper<Room>()
                .eq(Room::getBuildingId, buildingId)));
    }

    @GetMapping("/status-count")
    public Result<List<Map<String, Object>>> statusCount() {
        return Result.success(roomService.countByStatus());
    }

    @GetMapping("/{id}")
    public Result<Room> getById(@PathVariable Long id) {
        return Result.success(roomService.getById(id));
    }

    @PreAuthorize("hasAuthority('room:create')")
    @OperationLog(module = "Room Management", operation = "Create Room")
    @PostMapping
    public Result<Boolean> create(@Valid @RequestBody RoomDTO dto) {
        Room room = new Room();
        org.springframework.beans.BeanUtils.copyProperties(dto, room);
        if (dto.getStatus() != null) {
            room.setStatus(RoomStatus.fromCode(dto.getStatus()));
        }
        // Auto-generate image if not provided
        if (room.getImage() == null || room.getImage().isBlank()) {
            room.setImage("https://picsum.photos/seed/apt-room-" + System.currentTimeMillis() % 100000 + "/400/300");
        }
        return Result.success(roomService.save(room));
    }

    @PreAuthorize("hasAuthority('room:update')")
    @OperationLog(module = "Room Management", operation = "Update Room")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody RoomDTO dto) {
        Room room = roomService.getById(id);
        if (room == null) return Result.fail("Room not found");
        org.springframework.beans.BeanUtils.copyProperties(dto, room);
        if (dto.getStatus() != null) {
            room.setStatus(RoomStatus.fromCode(dto.getStatus()));
        }
        return Result.success(roomService.updateById(room));
    }

    @OperationLog(module = "Room Management", operation = "Change Room Status")
    @PutMapping("/{id}/status")
    public Result<Boolean> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        roomService.changeStatus(id, status);
        return Result.success();
    }

    @GetMapping("/discover")
    public Result<List<Map<String, Object>>> discover() {
        List<Room> vacantRooms = roomService.list(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, RoomStatus.VACANT)
                .orderByAsc(Room::getBuildingId)
                .orderByAsc(Room::getFloor));
        Map<Long, List<Room>> grouped = vacantRooms.stream()
                .collect(Collectors.groupingBy(Room::getBuildingId, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Room>> entry : grouped.entrySet()) {
            Building building = buildingService.getById(entry.getKey());
            Map<String, Object> group = new HashMap<>();
            group.put("buildingId", entry.getKey());
            group.put("buildingName", building != null ? building.getName() : "Unknown");
            group.put("rooms", entry.getValue());
            group.put("count", entry.getValue().size());
            result.add(group);
        }
        return Result.success(result);
    }

    @GetMapping("/{id}/similar")
    public Result<List<Room>> similar(@PathVariable Long id) {
        Room room = roomService.getById(id);
        if (room == null) return Result.success(Collections.emptyList());
        List<Room> similar = roomService.list(new LambdaQueryWrapper<Room>()
                .eq(Room::getBuildingId, room.getBuildingId())
                .eq(Room::getRoomTypeId, room.getRoomTypeId())
                .ne(Room::getId, id)
                .eq(Room::getStatus, RoomStatus.VACANT)
                .last("LIMIT 6"));
        return Result.success(similar);
    }

    @PreAuthorize("hasAuthority('room:delete')")
    @OperationLog(module = "Room Management", operation = "Delete Room")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        if (contractService.count(new LambdaQueryWrapper<Contract>().eq(Contract::getRoomId, id)
                .eq(Contract::getStatus, 1)) > 0) {
            return Result.fail("Cannot delete room with active contract");
        }
        return Result.success(roomService.removeById(id));
    }
}
