package com.apartment.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictDTO {
    @NotBlank(message = "Dict type is required")
    private String dictType;
    @NotBlank(message = "Dict code is required")
    private String dictCode;
    @NotBlank(message = "Dict label is required")
    private String dictLabel;
    private Integer sortOrder;
    private Integer status;
}
