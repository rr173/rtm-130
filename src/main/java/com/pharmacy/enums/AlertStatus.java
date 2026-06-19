package com.pharmacy.enums;

public enum AlertStatus {
    ACTIVE("活动中"),
    UPGRADED("已升级"),
    RESOLVED("已恢复"),
    CLOSED("已关闭");

    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
