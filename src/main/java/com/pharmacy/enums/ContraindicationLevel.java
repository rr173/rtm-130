package com.pharmacy.enums;

public enum ContraindicationLevel {
    SEVERE("严重禁忌"),
    MODERATE("中度禁忌"),
    MILD("轻度禁忌");

    private final String description;

    ContraindicationLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
