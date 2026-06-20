package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmacistPerformanceDTO {

    private String pharmacistId;
    private String pharmacistName;
    private Long totalReviewCount;
    private Long passedCount;
    private Long returnedCount;
    private Long keyAttentionCount;
    private Long timeoutCount;
    private BigDecimal averageReviewSeconds;
    private BigDecimal returnRate;
    private BigDecimal timeoutRate;
    private String period;
}
