package com.pharmacy.exception;

public class StockInsufficientException extends BusinessException {

    private final String drugCode;
    private final String drugName;
    private final int available;
    private final int required;

    public StockInsufficientException(String drugCode, String drugName, int available, int required) {
        super(409, String.format("药品[%s]库存不足，可用库存：%d，需要：%d", drugName, available, required));
        this.drugCode = drugCode;
        this.drugName = drugName;
        this.available = available;
        this.required = required;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequired() {
        return required;
    }
}
