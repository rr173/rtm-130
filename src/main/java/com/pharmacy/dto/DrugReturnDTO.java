package com.pharmacy.dto;

import com.pharmacy.entity.DrugReturn;
import com.pharmacy.entity.DrugReturnItem;
import com.pharmacy.entity.DrugReturnItemBatch;
import com.pharmacy.enums.DrugReturnStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DrugReturnDTO {

    private Long id;
    private String returnNo;
    private String prescriptionNo;
    private String patientId;
    private String patientName;
    private DrugReturnStatus status;
    private String statusDescription;
    private String returnReason;
    private String appliedBy;
    private LocalDateTime appliedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private String lossReason;
    private List<DrugReturnItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DrugReturnDTO fromEntity(DrugReturn drugReturn) {
        DrugReturnDTO dto = new DrugReturnDTO();
        dto.setId(drugReturn.getId());
        dto.setReturnNo(drugReturn.getReturnNo());
        dto.setPrescriptionNo(drugReturn.getPrescriptionNo());
        dto.setPatientId(drugReturn.getPatientId());
        dto.setPatientName(drugReturn.getPatientName());
        dto.setStatus(drugReturn.getStatus());
        dto.setStatusDescription(drugReturn.getStatus().getDescription());
        dto.setReturnReason(drugReturn.getReturnReason());
        dto.setAppliedBy(drugReturn.getAppliedBy());
        dto.setAppliedAt(drugReturn.getAppliedAt());
        dto.setReviewedBy(drugReturn.getReviewedBy());
        dto.setReviewedAt(drugReturn.getReviewedAt());
        dto.setReviewComment(drugReturn.getReviewComment());
        dto.setLossReason(drugReturn.getLossReason());
        dto.setCreatedAt(drugReturn.getCreatedAt());
        dto.setUpdatedAt(drugReturn.getUpdatedAt());

        if (drugReturn.getItems() != null) {
            dto.setItems(drugReturn.getItems().stream()
                    .map(DrugReturnDTO::convertItemToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private static DrugReturnItemDTO convertItemToDTO(DrugReturnItem item) {
        DrugReturnItemDTO dto = new DrugReturnItemDTO();
        dto.setId(item.getId());
        dto.setDrugCode(item.getDrugCode());
        dto.setDrugName(item.getDrugName());
        dto.setPrescriptionItemId(item.getPrescriptionItemId());
        dto.setReturnQuantity(item.getReturnQuantity());
        dto.setOriginalDispensedQuantity(item.getOriginalDispensedQuantity());
        dto.setReturnItemReason(item.getReturnItemReason());

        if (item.getBatchAllocations() != null) {
            dto.setBatchAllocations(item.getBatchAllocations().stream()
                    .map(DrugReturnDTO::convertBatchToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private static DrugReturnItemBatchDTO convertBatchToDTO(DrugReturnItemBatch batch) {
        DrugReturnItemBatchDTO dto = new DrugReturnItemBatchDTO();
        dto.setId(batch.getId());
        dto.setBatchId(batch.getBatchId());
        dto.setDrugCode(batch.getDrugCode());
        dto.setBatchNo(batch.getBatchNo());
        dto.setReturnQuantity(batch.getReturnQuantity());
        dto.setOriginalDispensedQuantity(batch.getOriginalDispensedQuantity());
        dto.setFromSplit(batch.getFromSplit());
        dto.setPending(batch.getPending());
        dto.setPendingReason(batch.getPendingReason());
        return dto;
    }
}
