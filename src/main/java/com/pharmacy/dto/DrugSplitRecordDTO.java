package com.pharmacy.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DrugSplitRecordDTO {

    private Long id;
    private Long batchId;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private Integer packageQuantity;
    private Integer splitUnit;
    private Integer splitQuantity;
    private Integer dispensedSplitQuantity;
    private Integer remainingSplitQuantity;
    private String prescriptionNo;
    private String operator;
    private String remark;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
