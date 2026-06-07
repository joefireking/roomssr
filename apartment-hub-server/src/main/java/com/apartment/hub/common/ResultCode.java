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

    // Business errors 1001-1999
    USER_EXISTS(1001, "Username already exists"),
    USER_NOT_FOUND(1002, "User not found"),
    ROOM_NOT_AVAILABLE(1003, "Room is not available"),
    ROOM_NOT_FOUND(1004, "Room not found"),
    CONTRACT_ACTIVE(1005, "Contract is already active"),
    CONTRACT_NOT_FOUND(1006, "Contract not found or not active"),
    BILL_ALREADY_PAID(1007, "Bill is already paid"),
    BILL_NOT_FOUND(1008, "Bill not found"),
    BILL_AMOUNT_MISMATCH(1009, "Payment amount does not match bill amount"),
    BILL_STATUS_CHANGED(1010, "Bill already paid or status changed"),
    INVALID_STATUS_TRANSITION(1011, "Invalid status transition"),
    PHONE_EXISTS(1012, "Phone number already exists"),
    IDCARD_EXISTS(1013, "ID card already exists"),
    TENANT_NOT_FOUND(1014, "Tenant not found"),
    TENANT_HAS_ACTIVE_CONTRACTS(1015, "Cannot delete tenant with active contracts"),
    ROOM_HAS_ACTIVE_CONTRACT(1016, "Cannot delete room with active contract"),
    INVALID_PAYMENT_METHOD(1017, "Invalid payment method"),
    REPAIR_ORDER_NOT_FOUND(1018, "Repair order not found or invalid status");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
