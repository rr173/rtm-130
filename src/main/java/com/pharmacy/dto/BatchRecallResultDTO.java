package com.pharmacy.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchRecallResultDTO {

    private String batchNo;
    private Integer lockedRemainingQuantity;
    private Integer affectedPatientCount;
    private Integer affectedPrescriptionCount;
    private List<AffectedPatientDTO> affectedPatients;
}
