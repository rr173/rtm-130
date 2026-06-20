package com.pharmacy.dto.consultation;

import com.pharmacy.enums.ConsultationOpinionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationOpinionDTO {

    private Long id;
    private String pharmacistId;
    private String pharmacistName;
    private ConsultationOpinionType opinionType;
    private String opinionTypeDescription;
    private String reason;
    private Boolean isPrimary;
    private LocalDateTime deadline;
    private LocalDateTime submittedAt;
    private Boolean isTimeout;
    private Boolean isAbstained;
    private LocalDateTime createdAt;
}
