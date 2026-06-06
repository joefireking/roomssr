package com.apartment.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class RoleDTO {
    @NotBlank(message = "Role name is required")
    private String roleName;
    @NotBlank(message = "Role code is required")
    private String roleCode;
    private String description;
    private Integer status;
    private List<Long> permissionIds;
}
