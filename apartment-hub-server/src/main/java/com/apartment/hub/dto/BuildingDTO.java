package com.apartment.hub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuildingDTO {
    @NotNull(message = "Apartment ID is required")
    private Long apartmentId;
    private String name;
    private Integer floors;
    private String description;
    private Integer status;
}
