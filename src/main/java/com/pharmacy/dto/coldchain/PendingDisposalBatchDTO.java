package com.pharmacy.dto.coldchain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PendingDisposalBatchDTO {

    private Long batchId;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private String pointCode;
    private String pointName;
    private Long alertId;
    private Integer availableQuantity;
    private LocalDateTime interruptionStartTime;
    private BigDecimal interruptionDurationMinutes;
    private BigDecimal maxToleranceMinutes;
    private BigDecimal maxRecordedTemperature;
    private BigDecimal minRecordedTemperature;
    private BigDecimal drugExtremeTemperature;
    private Boolean withinTolerance;
}
