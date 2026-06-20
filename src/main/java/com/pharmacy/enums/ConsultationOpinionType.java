package com.pharmacy.enums;

public enum ConsultationOpinionType {
    APPROVE("同意放行"),
    RETURN("建议退回"),
    KEY_ATTENTION("建议重点关注");

    private final String description;

    ConsultationOpinionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
