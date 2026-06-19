package com.pharmacy.enums;

public enum BatchStatus {
    NORMAL("正常"),
    NEAR_EXPIRY("近效期"),
    ABOUT_TO_EXPIRE("临期"),
    EXPIRED("已过期"),
    LOCKED("已锁定"),
    RECALLED("已召回");

    private final String description;

    BatchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
