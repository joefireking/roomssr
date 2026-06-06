package com.apartment.hub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderDTO {
    @NotNull(message = "Room ID is required")
    private Long roomId;
    private Long tenantId;
    @NotNull(message = "Repair type is required")
    private Integer type;
    private Integer priority = 1;
    private String description;
    private String images;
}
