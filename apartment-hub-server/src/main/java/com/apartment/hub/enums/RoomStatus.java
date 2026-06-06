package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RoomStatus {
    VACANT(0, "Vacant"),
    RENTED(1, "Rented"),
    MAINTENANCE(2, "Maintenance"),
    RESERVED(3, "Reserved");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    RoomStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
