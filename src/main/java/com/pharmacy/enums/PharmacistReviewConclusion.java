package com.pharmacy.enums;

public enum PharmacistReviewConclusion {
    PASSED("通过"),
    RETURNED_FOR_MODIFICATION("退回修改"),
    KEY_ATTENTION("重点关注通过");

    private final String description;

    PharmacistReviewConclusion(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
