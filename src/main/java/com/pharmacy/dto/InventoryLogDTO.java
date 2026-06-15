package com.pharmacy.dto;

import com.pharmacy.entity.InventoryLog;
import com.pharmacy.enums.InventoryLogType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogDTO {

    private Long id;
    private String drugCode;
    private String drugName;
    private InventoryLogType logType;
    private String logTypeDescription;
    private Integer quantity;
    private Integer beforeAvailableStock;
    private Integer afterAvailableStock;
    private Integer beforePreoccupiedStock;
    private Integer afterPreoccupiedStock;
    private String prescriptionNo;
    private String remark;
    private String operator;
    private LocalDateTime createdAt;

    public static InventoryLogDTO fromEntity(InventoryLog log) {
        InventoryLogDTO dto = new InventoryLogDTO();
        dto.setId(log.getId());
        dto.setDrugCode(log.getDrugCode());
        dto.setDrugName(log.getDrugName());
        dto.setLogType(log.getLogType());
        dto.setLogTypeDescription(log.getLogType().getDescription());
        dto.setQuantity(log.getQuantity());
        dto.setBeforeAvailableStock(log.getBeforeAvailableStock());
        dto.setAfterAvailableStock(log.getAfterAvailableStock());
        dto.setBeforePreoccupiedStock(log.getBeforePreoccupiedStock());
        dto.setAfterPreoccupiedStock(log.getAfterPreoccupiedStock());
        dto.setPrescriptionNo(log.getPrescriptionNo());
        dto.setRemark(log.getRemark());
        dto.setOperator(log.getOperator());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
