package com.apartment.hub.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PaymentMethod {
    CASH(0, "Cash"),
    BANK_TRANSFER(1, "Bank Transfer"),
    ALIPAY(2, "Alipay"),
    WECHAT(3, "WeChat");

    @EnumValue
    private final int code;
    @JsonValue
    private final String description;

    PaymentMethod(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
