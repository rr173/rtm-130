package com.pharmacy.dto.coldchain;

import com.pharmacy.enums.DisposalDecision;
import com.pharmacy.enums.MonitoringPointType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonitoringPointDTO {

    private Long id;
    private String pointCode;
    private String pointName;
    private MonitoringPointType pointType;
    private String pointTypeDescription;
    private String locationDescription;
    private BigDecimal tempMin;
    private BigDecimal tempMax;
    private BigDecimal humidityMin;
    private BigDecimal humidityMax;
    private Boolean enabled;
    private List<String> boundBatchNos;
    private BigDecimal currentTemperature;
    private BigDecimal currentHumidity;
    private String currentStatus;
}
