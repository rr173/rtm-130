package com.pharmacy.dto.consultation;

import com.pharmacy.enums.ConsultationConclusion;
import com.pharmacy.enums.ConsultationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConsultationDetailDTO {

    private Long id;
    private String prescriptionNo;
    private String patientId;
    private String patientName;
    private String diagnosisName;
    private String initiatorPharmacistId;
    private String initiatorPharmacistName;
    private String reason;
    private ConsultationStatus status;
    private String statusDescription;
    private ConsultationConclusion finalConclusion;
    private String finalConclusionDescription;
    private String summaryComments;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long totalDurationSeconds;
    private List<ConsultationOpinionDTO> opinions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
