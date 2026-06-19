package com.pharmacy.dto;

import com.pharmacy.entity.DispensingWindow;
import com.pharmacy.enums.DispensingWindowStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowDTO {

    private Long id;
    private String windowNo;
    private String windowName;
    private DispensingWindowStatus status;
    private String statusDescription;
    private String currentPrescriptionNo;
    private String currentPharmacistId;
    private String currentPharmacistName;
    private LocalDateTime dispenseStartTime;

    public static WindowDTO fromEntity(DispensingWindow window) {
        WindowDTO dto = new WindowDTO();
        dto.setId(window.getId());
        dto.setWindowNo(window.getWindowNo());
        dto.setWindowName(window.getWindowName());
        dto.setStatus(window.getStatus());
        dto.setStatusDescription(window.getStatus().getDescription());
        dto.setCurrentPrescriptionNo(window.getCurrentPrescriptionNo());
        dto.setCurrentPharmacistId(window.getCurrentPharmacistId());
        dto.setCurrentPharmacistName(window.getCurrentPharmacistName());
        dto.setDispenseStartTime(window.getDispenseStartTime());
        return dto;
    }
}
