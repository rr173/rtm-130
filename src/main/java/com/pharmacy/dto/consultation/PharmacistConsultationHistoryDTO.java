package com.pharmacy.dto.consultation;

import com.pharmacy.enums.ConsultationConclusion;
import com.pharmacy.enums.ConsultationOpinionType;
import com.pharmacy.enums.ConsultationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PharmacistConsultationHistoryDTO {

    private Long consultationId;
    private String prescriptionNo;
    private String patientName;
    private String diagnosisName;
    private String initiatorPharmacistName;
    private String reason;
    private ConsultationStatus status;
    private String statusDescription;
    private ConsultationConclusion finalConclusion;
    private String finalConclusionDescription;
    private ConsultationOpinionType myOpinion;
    private String myOpinionDescription;
    private Boolean isInitiator;
    private LocalDateTime participatedAt;
    private LocalDateTime completedAt;
}
