package com.pharmacy.enums;

public enum PrescriptionStatus {
    PENDING_REVIEW("待审核"),
    REVIEW_PASSED("审核通过"),
    REVIEW_WARNING("审核警告待确认"),
    REVIEW_BLOCKED("审核拦截"),
    PREOCCUPIED("已预占待配药"),
    PREOCCUPY_FAILED("预占失败"),
    DISPENSED("已发药"),
    CANCELLED("已取消");

    private final String description;

    PrescriptionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
