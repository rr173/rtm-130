package com.pharmacy.enums;

public enum ExpiryWarningLevel {
    NONE("无预警"),
    YELLOW("黄色预警-近效期"),
    RED("红色预警-临期");

    private final String description;

    ExpiryWarningLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
