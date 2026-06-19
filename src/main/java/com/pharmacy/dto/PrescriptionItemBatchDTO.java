package com.pharmacy.dto;

import lombok.Data;

@Data
public class PrescriptionItemBatchDTO {

    private Long id;
    private Long prescriptionItemId;
    private Long batchId;
    private String drugCode;
    private String batchNo;
    private Integer quantity;
    private Boolean fromSplit;
    private Long splitRecordId;
}
