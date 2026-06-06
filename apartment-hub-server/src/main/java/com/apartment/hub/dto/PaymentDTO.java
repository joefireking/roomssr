package com.apartment.hub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentDTO {
    @NotNull(message = "Bill ID is required")
    private Long billId;
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    @NotNull(message = "Payment method is required")
    private Integer paymentMethod;
    private String remark;
}
