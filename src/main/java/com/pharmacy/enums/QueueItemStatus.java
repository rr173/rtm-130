package com.pharmacy.enums;

public enum QueueItemStatus {
    WAITING("等待中"),
    PROCESSING("配药中"),
    COMPLETED("已完成"),
    RETURNED("已退回");

    private final String description;

    QueueItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
