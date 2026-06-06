package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RepairStatus {
    PENDING(0, "Pending"),
    ASSIGNED(1, "Assigned"),
    IN_PROGRESS(2, "In Progress"),
    COMPLETED(3, "Completed"),
    VERIFIED(4, "Verified");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    RepairStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
