package com.pharmacy.enums;

public enum InventoryLogType {
    STOCK_IN("入库"),
    PREOCCUPY("预占"),
    DISPENSE("发药扣减"),
    RELEASE("释放预占"),
    ADJUST("盘点修正");

    private final String description;

    InventoryLogType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
