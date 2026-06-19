package com.pharmacy.enums;

public enum BatchLogType {
    STOCK_IN("批次入库"),
    PREOCCUPY("批次预占"),
    RELEASE_PREOCCUPY("释放批次预占"),
    DISPENSE("批次发药"),
    SPLIT("拆零"),
    SPLIT_DISPENSE("拆零发药"),
    LOCK("锁定批次"),
    UNLOCK("解锁批次"),
    RECALL("批次召回"),
    EXPIRE_LOCK("过期自动锁定"),
    ADJUST("批次盘点修正");

    private final String description;

    BatchLogType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
