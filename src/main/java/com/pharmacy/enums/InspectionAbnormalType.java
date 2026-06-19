package com.pharmacy.enums;

public enum InspectionAbnormalType {
    NORMAL("正常"),
    FREQUENCY_ABNORMAL("上报频率不达标"),
    TREND_UP_ABNORMAL("温度持续上升趋势异常"),
    TREND_DOWN_ABNORMAL("温度持续下降趋势异常"),
    OFFLINE("传感器离线");

    private final String description;

    InspectionAbnormalType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
