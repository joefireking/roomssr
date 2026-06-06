package com.apartment.hub.mapper;

import com.apartment.hub.entity.Room;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {

    @Select("SELECT r.*, b.name as building_name, rt.type_name, a.name as apartment_name " +
            "FROM room r " +
            "LEFT JOIN building b ON r.building_id = b.id " +
            "LEFT JOIN room_type rt ON r.room_type_id = rt.id " +
            "LEFT JOIN apartment a ON b.apartment_id = a.id " +
            "WHERE r.deleted = 0 AND r.id = #{id}")
    Room selectRoomDetail(@Param("id") Long id);

    @Select("SELECT status, COUNT(*) as count FROM room WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();
}
