package com.pharmacy.enums;

public enum DisposalDecision {
    REINSPECTION_PASSED("复检通过"),
    REJECTED("报损");

    private final String description;

    DisposalDecision(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
