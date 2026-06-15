package com.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StockAdjustDTO {

    @NotBlank(message = "药品编码不能为空")
    private String drugCode;

    @NotNull(message = "修正后实际库存不能为空")
    private Integer actualStock;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @Size(max = 100, message = "操作人长度不能超过100")
    private String operator;
}
