package com.pharmacy.enums;

public enum PrescriptionStatus {
    PENDING_REVIEW("待自动审核"),
    REVIEW_PASSED("自动审核通过"),
    REVIEW_WARNING("自动审核警告"),
    REVIEW_BLOCKED("自动审核拦截"),
    PENDING_PHARMACIST_REVIEW("待药师审核"),
    IN_PHARMACIST_REVIEW("药师审核中"),
    IN_CONSULTATION("协同会诊中"),
    PENDING_MODIFICATION("待修改"),
    PHARMACIST_REVIEW_PASSED("药师审核通过"),
    KEY_ATTENTION("重点关注"),
    PREOCCUPIED("已预占待配药"),
    PREOCCUPY_FAILED("预占失败"),
    DISPENSING("配药中"),
    DISPENSE_READY("已配药待发药"),
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
