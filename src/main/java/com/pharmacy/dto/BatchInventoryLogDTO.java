package com.pharmacy.dto;

import com.pharmacy.enums.BatchLogType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchInventoryLogDTO {

    private Long id;
    private Long batchId;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private BatchLogType logType;
    private String logTypeDescription;
    private Integer quantity;
    private Integer beforeAvailable;
    private Integer afterAvailable;
    private Integer beforePreoccupied;
    private Integer afterPreoccupied;
    private Integer beforeSplit;
    private Integer afterSplit;
    private String prescriptionNo;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;
}
