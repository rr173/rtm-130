package com.pharmacy.enums;

public enum DrugReturnStatus {
    PENDING_REVIEW("待审核"),
    APPROVED("审核通过(已回库)"),
    REJECTED("审核拒绝(已报损)"),
    CANCELLED("已取消");

    private final String description;

    DrugReturnStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
