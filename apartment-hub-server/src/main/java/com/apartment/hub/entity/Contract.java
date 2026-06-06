package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.apartment.hub.enums.ContractStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {
    private String contractNo;
    private Long tenantId;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private Integer paymentCycle;
    private ContractStatus status;
    private String terminateReason;
    private String remark;
}
