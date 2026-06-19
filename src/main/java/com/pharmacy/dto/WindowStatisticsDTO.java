package com.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowStatisticsDTO {

    private String windowNo;
    private String windowName;
    private Long todayCount;
    private Double avgDurationSeconds;
    private String avgDurationFormatted;
}
