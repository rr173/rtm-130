package com.pharmacy.dto.reviewrule;

import com.pharmacy.enums.ReviewResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrayReleaseReportDTO {

    private Long grayReleaseId;
    private Integer newConfigVersion;
    private Integer baseConfigVersion;
    private long totalReviewed;
    private long consistentCount;
    private long inconsistentCount;
    private double consistencyRate;
    private long moreBlockedCount;
    private long fewerBlockedCount;
    private Map<ReviewResultType, Long> oldRuleResultDistribution;
    private Map<ReviewResultType, Long> newRuleResultDistribution;
    private LocalDateTime reportGeneratedAt;
}
