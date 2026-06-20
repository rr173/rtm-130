package com.pharmacy.dto.reviewrule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRuleChangeLogDTO {

    private Long id;
    private Integer configVersion;
    private String fieldName;
    private String fieldDisplayName;
    private String oldValue;
    private String newValue;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime effectiveAt;
}
