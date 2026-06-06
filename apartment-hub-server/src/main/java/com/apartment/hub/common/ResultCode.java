package com.apartment.hub.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    INTERNAL_ERROR(500, "Internal server error"),
    USER_EXISTS(1001, "Username already exists"),
    ROOM_NOT_AVAILABLE(1002, "Room is not available"),
    CONTRACT_ACTIVE(1003, "Contract is already active"),
    BILL_ALREADY_PAID(1004, "Bill is already paid"),
    INVALID_STATUS_TRANSITION(1005, "Invalid status transition"),
    PHONE_EXISTS(1006, "Phone number already exists"),
    IDCARD_EXISTS(1007, "ID card already exists");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
