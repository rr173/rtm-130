package com.pharmacy.enums;

public enum ConsultationConclusion {
    PASSED("通过"),
    RETURNED("退回"),
    KEY_ATTENTION("重点关注通过");

    private final String description;

    ConsultationConclusion(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
