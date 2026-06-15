package com.pharmacy.dto;

import com.pharmacy.enums.ReviewResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultDTO {

    private ReviewResultType overallResult;

    private List<ReviewRuleResultDTO> ruleResults;

    private String overallMessage;

    public boolean isPassed() {
        return overallResult == ReviewResultType.PASSED;
    }

    public boolean isWarning() {
        return overallResult == ReviewResultType.WARNING;
    }

    public boolean isBlocked() {
        return overallResult == ReviewResultType.BLOCKED;
    }
}
