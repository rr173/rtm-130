package com.pharmacy.dto.coldchain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchEnvironmentHistoryDTO {

    private String batchNo;
    private String drugCode;
    private String drugName;
    private List<EnvironmentAbnormalRecord> abnormalRecords;

    @Data
    public static class EnvironmentAbnormalRecord {
        private Long alertId;
        private String pointCode;
        private String pointName;
        private String alertLevel;
        private String alertLevelDescription;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal maxTemperature;
        private BigDecimal minTemperature;
        private BigDecimal maxHumidity;
        private BigDecimal minHumidity;
        private Long durationMinutes;
        private String disposalDecision;
        private String disposalDecisionDescription;
    }
}
