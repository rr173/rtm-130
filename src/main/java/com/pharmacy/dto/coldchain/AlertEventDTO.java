package com.pharmacy.dto.coldchain;

import com.pharmacy.enums.AlertLevel;
import com.pharmacy.enums.AlertStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertEventDTO {

    private Long id;
    private String pointCode;
    private String pointName;
    private AlertLevel alertLevel;
    private String alertLevelDescription;
    private AlertStatus alertStatus;
    private String alertStatusDescription;
    private BigDecimal triggerTemperature;
    private BigDecimal triggerHumidity;
    private BigDecimal tempLowerLimit;
    private BigDecimal tempUpperLimit;
    private BigDecimal humidityLowerLimit;
    private BigDecimal humidityUpperLimit;
    private LocalDateTime firstTriggerTime;
    private LocalDateTime lastTriggerTime;
    private LocalDateTime resolvedTime;
    private Long parentAlertId;
    private String description;
    private Long durationMinutes;
}
