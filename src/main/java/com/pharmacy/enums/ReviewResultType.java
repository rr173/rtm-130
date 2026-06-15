package com.pharmacy.enums;

public enum ReviewResultType {
    PASSED("通过"),
    WARNING("警告放行"),
    BLOCKED("拦截");

    private final String description;

    ReviewResultType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
