package com.pharmacy.dto.consultation;

import com.pharmacy.enums.ConsultationStatus;
import com.pharmacy.enums.PrescriptionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationTodoItemDTO {

    private Long consultationId;
    private String prescriptionNo;
    private String patientId;
    private String patientName;
    private String diagnosisName;
    private String initiatorPharmacistName;
    private String reason;
    private PrescriptionType prescriptionType;
    private String prescriptionTypeDescription;
    private ConsultationStatus status;
    private String statusDescription;
    private LocalDateTime deadline;
    private Long remainingSeconds;
    private LocalDateTime createdAt;
}
