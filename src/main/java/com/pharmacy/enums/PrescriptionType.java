package com.pharmacy.enums;

public enum PrescriptionType {
    NORMAL("普通"),
    EMERGENCY("急诊"),
    NARCOTIC("麻精");

    private final String description;

    PrescriptionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
