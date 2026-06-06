package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RepairType {
    PLUMBING(0, "Plumbing"),
    FURNITURE(1, "Furniture"),
    APPLIANCE(2, "Appliance"),
    NETWORK(3, "Network"),
    OTHER(4, "Other");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    RepairType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
