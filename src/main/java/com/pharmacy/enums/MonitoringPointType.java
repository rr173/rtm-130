package com.pharmacy.enums;

public enum MonitoringPointType {
    REFRIGERATOR("冷藏柜"),
    COOL_CABINET("阴凉柜"),
    NORMAL_AREA("常温区"),
    FREEZER("冷冻柜");

    private final String description;

    MonitoringPointType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
