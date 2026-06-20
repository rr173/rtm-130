package com.pharmacy.enums;

public enum GrayReleaseStatus {
    PENDING("待发布"),
    GRAYING("灰度中"),
    FULLY_RELEASED("全量发布"),
    CANCELLED("已取消");

    private final String description;

    GrayReleaseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
