package com.apartment.hub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoomDTO {
    @NotNull(message = "Building ID is required")
    private Long buildingId;
    private Long roomTypeId;
    private String roomNumber;
    private Integer floor;
    private BigDecimal rentPrice;
    private String image;
    private Integer status;
}
