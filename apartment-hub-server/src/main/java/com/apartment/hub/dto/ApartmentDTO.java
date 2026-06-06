package com.apartment.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApartmentDTO {
    @NotBlank(message = "Apartment name is required")
    private String name;
    private String address;
    private String city;
    private String district;
    private String contactName;
    private String contactPhone;
    private String description;
    private Integer status;
}
