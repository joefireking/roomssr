package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.apartment.hub.enums.RoomStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room")
public class Room extends BaseEntity {
    private Long buildingId;
    private Long roomTypeId;
    private String roomNumber;
    private Integer floor;
    private BigDecimal rentPrice;
    private String image;
    private RoomStatus status;
}
