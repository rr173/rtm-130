package com.pharmacy.dto.pharmacistreview;

import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTodoItemDTO {

    private Long id;
    private String prescriptionNo;
    private String patientId;
    private String patientName;
    private String diagnosisName;
    private String doctorName;
    private String department;
    private PrescriptionType type;
    private String typeDescription;
    private PrescriptionStatus status;
    private String statusDescription;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime reviewDeadline;
    private Long remainingSeconds;
    private Boolean isEmergency;
    private Boolean hasWarning;
}
