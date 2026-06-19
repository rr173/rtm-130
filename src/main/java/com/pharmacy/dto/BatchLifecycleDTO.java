package com.pharmacy.dto;

import com.pharmacy.enums.BatchStatus;
import com.pharmacy.enums.ExpiryWarningLevel;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchLifecycleDTO {

    private Long batchId;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private BatchStatus status;
    private ExpiryWarningLevel warningLevel;
    private List<BatchInventoryLogDTO> logs;
}
