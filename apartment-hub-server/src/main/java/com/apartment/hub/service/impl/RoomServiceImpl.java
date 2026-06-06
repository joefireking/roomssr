package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.entity.Room;
import com.apartment.hub.enums.RoomStatus;
import com.apartment.hub.mapper.RoomMapper;
import com.apartment.hub.service.RoomService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    @Override
    @Transactional
    public void changeStatus(Long roomId, Integer newStatus) {
        Room room = getById(roomId);
        if (room == null) {
            throw new BusinessException("Room not found");
        }
        RoomStatus[] values = RoomStatus.values();
        if (newStatus < 0 || newStatus >= values.length) {
            throw new BusinessException("Invalid room status: " + newStatus);
        }
        room.setStatus(values[newStatus]);
        updateById(room);
    }

    @Override
    public List<Map<String, Object>> countByStatus() {
        return baseMapper.countByStatus();
    }
}
