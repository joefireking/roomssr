package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RepairPriority {
    URGENT(0, "Urgent"),
    NORMAL(1, "Normal"),
    LOW(2, "Low");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    RepairPriority(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
