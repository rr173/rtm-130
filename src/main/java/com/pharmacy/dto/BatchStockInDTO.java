package com.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchStockInDTO {

    @NotBlank(message = "药品编码不能为空")
    private String drugCode;

    @NotBlank(message = "批号不能为空")
    @Size(max = 100, message = "批号长度不能超过100")
    private String batchNo;

    @NotNull(message = "生产日期不能为空")
    private LocalDate productionDate;

    @NotNull(message = "有效期不能为空")
    private LocalDate expiryDate;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量最少为1")
    private Integer quantity;

    private BigDecimal purchasePrice;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @Size(max = 100, message = "操作人长度不能超过100")
    private String operator;
}
