package com.apartment.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    @NotBlank(message = "Username is required")
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private List<Long> roleIds;
}
