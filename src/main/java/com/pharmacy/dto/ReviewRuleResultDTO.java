package com.pharmacy.dto;

import com.pharmacy.enums.ReviewResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRuleResultDTO {

    private String ruleCode;

    private String ruleName;

    private ReviewResultType result;

    private String message;

    private String drugCode;

    private String drugName;

    private String relatedDrugCode;

    private String relatedDrugName;
}
