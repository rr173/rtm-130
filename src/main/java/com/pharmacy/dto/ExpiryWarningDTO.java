package com.pharmacy.dto;

import com.pharmacy.enums.ExpiryWarningLevel;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpiryWarningDTO {

    private Long batchId;
    private String drugCode;
    private String drugName;
    private String batchNo;
    private LocalDate expiryDate;
    private Long daysToExpiry;
    private Integer remainingQuantity;
    private ExpiryWarningLevel warningLevel;
}
