package com.pharmacy.dto.coldchain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TempHumidityReadingDTO {

    private Long id;
    private String pointCode;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private LocalDateTime collectTime;
    private Boolean tempOutOfRange;
    private Boolean humidityOutOfRange;
    private String remark;
}
