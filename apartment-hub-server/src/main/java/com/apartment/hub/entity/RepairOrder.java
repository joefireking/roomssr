package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.apartment.hub.enums.RepairPriority;
import com.apartment.hub.enums.RepairStatus;
import com.apartment.hub.enums.RepairType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("repair_order")
public class RepairOrder extends BaseEntity {
    private String orderNo;
    private Long roomId;
    private Long tenantId;
    private RepairType type;
    private RepairPriority priority;
    private String description;
    private String images;
    private RepairStatus status;
    private Long assigneeId;
    private LocalDateTime resolveTime;
    private String resolveRemark;
}
