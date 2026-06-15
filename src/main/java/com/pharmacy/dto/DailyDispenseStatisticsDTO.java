package com.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyDispenseStatisticsDTO {

    private String drugCode;
    private String drugName;
    private LocalDate date;
    private Integer totalQuantity;
}
