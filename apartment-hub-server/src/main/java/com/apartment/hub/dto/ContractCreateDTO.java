package com.apartment.hub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractCreateDTO {
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    @NotNull(message = "Room ID is required")
    private Long roomId;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    @NotNull(message = "Rent amount is required")
    private BigDecimal rentAmount;
    @NotNull(message = "Deposit amount is required")
    private BigDecimal depositAmount;
    private Integer paymentCycle = 1;
    private String remark;
}
