package com.pharmacy.enums;

public enum DispensingWindowStatus {
    IDLE("空闲"),
    BUSY("忙碌"),
    CLOSED("关闭");

    private final String description;

    DispensingWindowStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
