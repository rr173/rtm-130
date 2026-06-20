package com.pharmacy.dto.reviewrule;

import com.pharmacy.enums.ReviewResultType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewRuleConfigUpdateDTO {

    @NotNull(message = "单品极量倍数不能为空")
    @DecimalMin(value = "0.1", message = "单品极量倍数最小为0.1")
    private BigDecimal maxSingleDoseMultiplier;

    @NotNull(message = "重复用药时间窗口不能为空")
    @Min(value = 1, message = "重复用药时间窗口最小为1小时")
    private Integer duplicateMedicationWindowHours;

    @NotNull(message = "严重禁忌处理策略不能为空")
    private ReviewResultType severeContraindicationAction;

    @NotNull(message = "中度禁忌处理策略不能为空")
    private ReviewResultType moderateContraindicationAction;

    @NotNull(message = "轻度禁忌处理策略不能为空")
    private ReviewResultType mildContraindicationAction;

    private String description;

    private String operator;

    private String remark;
}
