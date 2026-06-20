package com.pharmacy.dto.consultation;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConsultationStatisticsDTO {

    private LocalDate date;
    private Integer consultationCount;
    private Double avgDurationMinutes;
}
