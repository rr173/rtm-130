package com.pharmacy.enums;

public enum AlertLevel {
    YELLOW("黄色预警", 1),
    ORANGE("橙色告警", 2),
    RED("红色严重告警", 3);

    private final String description;
    private final int severity;

    AlertLevel(String description, int severity) {
        this.description = description;
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public int getSeverity() {
        return severity;
    }

    public AlertLevel upgrade() {
        return switch (this) {
            case YELLOW -> ORANGE;
            case ORANGE -> RED;
            case RED -> RED;
        };
    }
}
