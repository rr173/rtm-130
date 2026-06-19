package com.pharmacy.dto;

import com.pharmacy.entity.DispensingWindow;
import com.pharmacy.enums.DispenseChannel;
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
    private DispenseChannel serviceChannel;
    private String serviceChannelDescription;
    private DispenseChannel currentServingChannel;
    private String currentServingChannelDescription;
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
        dto.setServiceChannel(window.getServiceChannel());
        dto.setServiceChannelDescription(window.getServiceChannel().getDescription());
        if (window.getCurrentServingChannel() != null) {
            dto.setCurrentServingChannel(window.getCurrentServingChannel());
            dto.setCurrentServingChannelDescription(window.getCurrentServingChannel().getDescription());
        }
        dto.setCurrentPrescriptionNo(window.getCurrentPrescriptionNo());
        dto.setCurrentPharmacistId(window.getCurrentPharmacistId());
        dto.setCurrentPharmacistName(window.getCurrentPharmacistName());
        dto.setDispenseStartTime(window.getDispenseStartTime());
        return dto;
    }
}
