package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_type")
public class RoomType extends BaseEntity {
    private String typeName;
    private BigDecimal area;
    private String orientation;
    private BigDecimal basePrice;
    private String facilities;
    private String description;
}
