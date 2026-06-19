package com.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueuePositionDTO {

    private String prescriptionNo;
    private Integer position;
    private Integer estimatedWaitMinutes;
    private Integer queueLength;
}
