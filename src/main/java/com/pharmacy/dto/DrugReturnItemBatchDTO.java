package com.pharmacy.dto;

import lombok.Data;

@Data
public class DrugReturnItemBatchDTO {

    private Long id;
    private Long batchId;
    private String drugCode;
    private String batchNo;
    private Integer returnQuantity;
    private Integer originalDispensedQuantity;
    private Boolean fromSplit;
    private Boolean pending;
    private String pendingReason;
}
