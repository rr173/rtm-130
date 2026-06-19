package com.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatisticsDTO {

    private String timeSlot;
    private Integer peakQueueLength;
    private Double avgWaitMinutes;
}
