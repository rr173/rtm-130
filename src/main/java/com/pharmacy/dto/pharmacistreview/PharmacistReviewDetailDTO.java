package com.pharmacy.dto.pharmacistreview;

import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmacistReviewDetailDTO {

    private Long id;
    private String prescriptionNo;
    private String patientId;
    private String patientName;
    private String diagnosisCode;
    private String diagnosisName;
    private String doctorId;
    private String doctorName;
    private String department;
    private PrescriptionType type;
    private String typeDescription;
    private PrescriptionStatus status;
    private String statusDescription;
    private List<com.pharmacy.dto.PrescriptionItemDTO> items;
    private String reviewComments;
    private String claimedByPharmacistId;
    private String claimedByPharmacistName;
    private LocalDateTime claimedAt;
    private LocalDateTime reviewDeadline;
    private Long remainingSeconds;
    private List<PatientRecentDrugDTO> recentDrugs;
    private IngredientOverlapWarningDTO ingredientOverlapWarning;
    private Boolean isKeyAttention;
    private String pharmacistAttentionReason;
    private LocalDateTime createdAt;
}
