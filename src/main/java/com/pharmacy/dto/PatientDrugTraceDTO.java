package com.pharmacy.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientDrugTraceDTO {

    private String patientId;
    private String patientName;
    private String prescriptionNo;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private Integer quantity;
    private Boolean fromSplit;
    private LocalDateTime dispensedAt;
    private String dispensedBy;
}
