package com.pharmacy.dto.reviewrule;

import com.pharmacy.enums.ReviewResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRuleConfigDTO {

    private Long id;
    private Integer version;
    private BigDecimal maxSingleDoseMultiplier;
    private Integer duplicateMedicationWindowHours;
    private ReviewResultType severeContraindicationAction;
    private ReviewResultType moderateContraindicationAction;
    private ReviewResultType mildContraindicationAction;
    private String description;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
