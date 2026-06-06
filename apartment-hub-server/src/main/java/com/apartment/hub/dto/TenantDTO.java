package com.apartment.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TenantDTO {
    @NotBlank(message = "Name is required")
    private String name;
    private Integer gender;
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    private String phone;
    @NotBlank(message = "ID card is required")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "Invalid ID card number")
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String emergencyContact;
    private String emergencyPhone;
    private String tag;
    private String remark;
}
