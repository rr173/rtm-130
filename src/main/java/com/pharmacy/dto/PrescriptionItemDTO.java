package com.pharmacy.dto;

import com.pharmacy.enums.UsageType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrescriptionItemDTO {

    @NotBlank(message = "药品编码不能为空")
    @Size(max = 50, message = "药品编码长度不能超过50")
    private String drugCode;

    @NotBlank(message = "药品名称不能为空")
    @Size(max = 200, message = "药品名称长度不能超过200")
    private String drugName;

    @NotNull(message = "单次剂量不能为空")
    @DecimalMin(value = "0.01", message = "单次剂量必须大于0")
    @DecimalMax(value = "9999.99", message = "单次剂量过大")
    private BigDecimal singleDose;

    @Size(max = 20, message = "剂量单位长度不能超过20")
    private String doseUnit;

    @NotNull(message = "用法不能为空")
    private UsageType usage;

    @NotBlank(message = "频次不能为空")
    @Size(max = 20, message = "频次长度不能超过20")
    private String frequency;

    @NotNull(message = "天数不能为空")
    @Min(value = 1, message = "天数最少为1天")
    @Max(value = 365, message = "天数最多为365天")
    private Integer days;

    @NotNull(message = "总量不能为空")
    @Min(value = 1, message = "总量最少为1")
    private Integer totalQuantity;

    @Size(max = 200, message = "配药说明长度不能超过200")
    private String dispensingNotes;
}
