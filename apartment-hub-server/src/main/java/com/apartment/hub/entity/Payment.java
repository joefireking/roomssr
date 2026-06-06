package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.apartment.hub.enums.PaymentMethod;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment")
public class Payment extends BaseEntity {
    private String paymentNo;
    private Long billId;
    private Long tenantId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDateTime paymentTime;
    private String remark;
    private Long operatorId;
}
