package com.pharmacy.dto.coldchain;

import com.pharmacy.enums.InspectionAbnormalType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InspectionReportItemDTO {

    private Long id;
    private String pointCode;
    private String pointName;
    private Boolean isNormal;
    private List<InspectionAbnormalType> abnormalTypes;
    private List<String> abnormalTypeDescriptions;
    private Integer expectedReadingCount;
    private Integer actualReadingCount;
    private BigDecimal frequencyRatio;
    private BigDecimal firstTemperature;
    private BigDecimal lastTemperature;
    private BigDecimal temperatureDifference;
    private String remark;
}
