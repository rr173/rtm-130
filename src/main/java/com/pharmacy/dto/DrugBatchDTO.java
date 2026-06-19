package com.pharmacy.dto;

import com.pharmacy.enums.BatchStatus;
import com.pharmacy.enums.ExpiryWarningLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DrugBatchDTO {

    private Long id;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal purchasePrice;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer preoccupiedQuantity;
    private Integer splitQuantity;
    private Integer dispensedQuantity;
    private Integer remainingQuantity;
    private BatchStatus status;
    private ExpiryWarningLevel warningLevel;
    private Long daysToExpiry;
    private Boolean splitLocked;
    private String splitLockedBy;
    private LocalDateTime splitLockedAt;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
