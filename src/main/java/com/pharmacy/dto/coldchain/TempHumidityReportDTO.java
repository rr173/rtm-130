package com.pharmacy.dto.coldchain;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TempHumidityReportDTO {

    @NotBlank(message = "监控点编号不能为空")
    private String pointCode;

    @NotNull(message = "温度值不能为空")
    @DecimalMin(value = "-50.00", message = "温度值超出合理范围")
    @DecimalMax(value = "80.00", message = "温度值超出合理范围")
    private BigDecimal temperature;

    @NotNull(message = "湿度值不能为空")
    @DecimalMin(value = "0.00", message = "湿度值不能小于0%")
    @DecimalMax(value = "100.00", message = "湿度值不能超过100%")
    private BigDecimal humidity;

    @NotNull(message = "采集时间不能为空")
    private LocalDateTime collectTime;

    private String remark;
}
