package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.apartment.hub.enums.BillStatus;
import com.apartment.hub.enums.BillType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bill")
public class Bill extends BaseEntity {
    private String billNo;
    private Long contractId;
    private Long tenantId;
    private Long roomId;
    private BillType billType;
    private BigDecimal amount;
    private String billingMonth;
    private LocalDate dueDate;
    private BillStatus status;
    private LocalDateTime paidTime;
    private String remark;
}
