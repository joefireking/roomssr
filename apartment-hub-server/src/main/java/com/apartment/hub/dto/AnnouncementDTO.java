package com.apartment.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementDTO {
    @NotBlank(message = "Title is required")
    private String title;
    private String content;
    private String summary;
    private Integer status;
    private Integer topFlag;
}
