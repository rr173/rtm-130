package com.pharmacy.enums;

import java.util.Arrays;
import java.util.List;

public enum DispenseChannel {
    FAST("快速通道", "3种及以下药品"),
    NORMAL("普通通道", "4种及以上药品"),
    BOTH("双通道", "同时服务快速和普通通道");

    private final String description;
    private final String rule;

    DispenseChannel(String description, String rule) {
        this.description = description;
        this.rule = rule;
    }

    public String getDescription() {
        return description;
    }

    public String getRule() {
        return rule;
    }

    public static DispenseChannel classifyByDrugCount(int drugCount) {
        return drugCount <= 3 ? FAST : NORMAL;
    }

    public boolean canServe(DispenseChannel itemChannel) {
        if (this == BOTH) {
            return true;
        }
        return this == itemChannel;
    }

    public List<DispenseChannel> getServiceableChannels() {
        if (this == BOTH) {
            return Arrays.asList(FAST, NORMAL);
        }
        return Arrays.asList(this);
    }
}
