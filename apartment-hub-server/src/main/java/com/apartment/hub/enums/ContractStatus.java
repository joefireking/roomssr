package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ContractStatus {
    DRAFT(0, "Draft"),
    ACTIVE(1, "Active"),
    EXPIRED(2, "Expired"),
    TERMINATED(3, "Terminated");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    ContractStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
