package com.pharmacy.dto;

import com.pharmacy.entity.DispenseQueueItem;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.enums.QueueItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueItemDTO {

    private Long id;
    private String prescriptionNo;
    private PrescriptionType prescriptionType;
    private String prescriptionTypeDescription;
    private LocalDateTime enqueueTime;
    private QueueItemStatus status;
    private String statusDescription;
    private String windowNo;
    private LocalDateTime claimTime;
    private Integer returnCount;
    private Integer position;
    private Integer estimatedWaitMinutes;

    public static QueueItemDTO fromEntity(DispenseQueueItem item) {
        QueueItemDTO dto = new QueueItemDTO();
        dto.setId(item.getId());
        dto.setPrescriptionNo(item.getPrescriptionNo());
        dto.setPrescriptionType(item.getPrescriptionType());
        dto.setPrescriptionTypeDescription(item.getPrescriptionType().getDescription());
        dto.setEnqueueTime(item.getEnqueueTime());
        dto.setStatus(item.getStatus());
        dto.setStatusDescription(item.getStatus().getDescription());
        dto.setWindowNo(item.getWindowNo());
        dto.setClaimTime(item.getClaimTime());
        dto.setReturnCount(item.getReturnCount());
        return dto;
    }
}
