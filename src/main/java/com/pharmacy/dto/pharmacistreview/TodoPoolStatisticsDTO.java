package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoPoolStatisticsDTO {

    private String timePeriod;
    private Integer peakPendingCount;
    private BigDecimal averageWaitingMinutes;
    private Long totalReviewedCount;
}
