package com.apartment.hub.service;

import com.apartment.hub.entity.Room;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;

public interface RoomService extends IService<Room> {
    void changeStatus(Long roomId, Integer newStatus);
    List<Map<String, Object>> countByStatus();
}
