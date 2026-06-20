package com.pharmacy.enums;

public enum ConsultationStatus {
    IN_PROGRESS("会诊中"),
    COMPLETED("已完成");

    private final String description;

    ConsultationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
