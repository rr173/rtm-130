package com.pharmacy.dto;

import lombok.Data;

import java.util.List;

@Data
public class DrugReturnItemDTO {

    private Long id;
    private String drugCode;
    private String drugName;
    private Long prescriptionItemId;
    private Integer returnQuantity;
    private Integer originalDispensedQuantity;
    private String returnItemReason;
    private List<DrugReturnItemBatchDTO> batchAllocations;
}
