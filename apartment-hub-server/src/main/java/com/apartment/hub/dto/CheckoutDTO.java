package com.apartment.hub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckoutDTO {
    @NotNull(message = "Contract ID is required")
    private Long contractId;
    private BigDecimal damageCost;
    private BigDecimal penaltyAmount;
    private String terminateReason;
    private List<DamageItem> damageItems;

    @Data
    public static class DamageItem {
        private String itemName;
        private BigDecimal cost;
        private String description;
    }
}
