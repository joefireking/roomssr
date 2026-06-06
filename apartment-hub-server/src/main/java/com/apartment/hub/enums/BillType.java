package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BillType {
    RENT(0, "Rent"),
    DEPOSIT(1, "Deposit"),
    UTILITY(2, "Utility"),
    PROPERTY(3, "Property");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    BillType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
