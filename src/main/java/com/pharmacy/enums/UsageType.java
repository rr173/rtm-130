package com.pharmacy.enums;

public enum UsageType {
    ORAL("口服"),
    INJECTION("注射"),
    EXTERNAL("外用"),
    INHALATION("吸入"),
    RECTAL("直肠"),
    OPHTHALMIC("眼用"),
    OTIC("耳用"),
    NASAL("鼻用");

    private final String description;

    UsageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
