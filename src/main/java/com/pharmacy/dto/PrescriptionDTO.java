package com.pharmacy.dto;

import com.pharmacy.entity.Prescription;
import com.pharmacy.entity.PrescriptionItem;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDTO {

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
    private PrescriptionStatus status;
    private String statusDescription;
    private List<PrescriptionItemDTO> items;
    private String reviewComments;
    private String lackDrugDetails;
    private String dispensedBy;
    private LocalDateTime dispensedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PrescriptionDTO fromEntity(Prescription prescription) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setId(prescription.getId());
        dto.setPrescriptionNo(prescription.getPrescriptionNo());
        dto.setPatientId(prescription.getPatientId());
        dto.setPatientName(prescription.getPatientName());
        dto.setDiagnosisCode(prescription.getDiagnosisCode());
        dto.setDiagnosisName(prescription.getDiagnosisName());
        dto.setDoctorId(prescription.getDoctorId());
        dto.setDoctorName(prescription.getDoctorName());
        dto.setDepartment(prescription.getDepartment());
        dto.setType(prescription.getType());
        dto.setStatus(prescription.getStatus());
        dto.setStatusDescription(prescription.getStatus().getDescription());
        dto.setReviewComments(prescription.getReviewComments());
        dto.setLackDrugDetails(prescription.getLackDrugDetails());
        dto.setDispensedBy(prescription.getDispensedBy());
        dto.setDispensedAt(prescription.getDispensedAt());
        dto.setCancelledAt(prescription.getCancelledAt());
        dto.setCancelReason(prescription.getCancelReason());
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setUpdatedAt(prescription.getUpdatedAt());

        if (prescription.getItems() != null) {
            dto.setItems(prescription.getItems().stream()
                    .map(PrescriptionDTO::convertItemToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private static PrescriptionItemDTO convertItemToDTO(PrescriptionItem item) {
        PrescriptionItemDTO dto = new PrescriptionItemDTO();
        dto.setDrugCode(item.getDrugCode());
        dto.setDrugName(item.getDrugName());
        dto.setSingleDose(item.getSingleDose());
        dto.setDoseUnit(item.getDoseUnit());
        dto.setUsage(item.getUsage());
        dto.setFrequency(item.getFrequency());
        dto.setDays(item.getDays());
        dto.setTotalQuantity(item.getTotalQuantity());
        dto.setDispensingNotes(item.getDispensingNotes());
        return dto;
    }
}
