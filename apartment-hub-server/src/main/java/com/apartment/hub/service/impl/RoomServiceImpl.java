package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.common.ResultCode;
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
    public void changeStatus(Long roomId, Integer newStatusCode) {
        Room room = getById(roomId);
        if (room == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        RoomStatus target = RoomStatus.fromCode(newStatusCode);
        RoomStatus current = room.getStatus();

        // State transition validation
        if (!isValidTransition(current, target)) {
            throw new BusinessException(ResultCode.INVALID_STATUS_TRANSITION);
        }

        room.setStatus(target);
        updateById(room);
    }

    private boolean isValidTransition(RoomStatus from, RoomStatus to) {
        return switch (from) {
            case VACANT -> to == RoomStatus.RENTED || to == RoomStatus.MAINTENANCE || to == RoomStatus.RESERVED;
            case RENTED -> to == RoomStatus.VACANT;
            case MAINTENANCE -> to == RoomStatus.VACANT;
            case RESERVED -> to == RoomStatus.VACANT || to == RoomStatus.RENTED;
        };
    }

    @Override
    public List<Map<String, Object>> countByStatus() {
        return baseMapper.countByStatus();
    }
}
