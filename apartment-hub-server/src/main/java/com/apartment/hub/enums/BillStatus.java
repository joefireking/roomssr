package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BillStatus {
    PENDING(0, "Pending"),
    PAID(1, "Paid"),
    OVERDUE(2, "Overdue"),
    CANCELLED(3, "Cancelled");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    BillStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
