package com.pharmacy.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AffectedPatientDTO {

    private String patientId;
    private String patientName;
    private String prescriptionNo;
    private LocalDateTime dispensedAt;
    private String dispensedBy;
    private Integer dispensedQuantity;
}
