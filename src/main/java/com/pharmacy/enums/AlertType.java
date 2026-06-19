package com.pharmacy.enums;

public enum AlertType {
    TEMPERATURE_HUMIDITY("温湿度告警"),
    SENSOR_OFFLINE("传感器离线告警");

    private final String description;

    AlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
