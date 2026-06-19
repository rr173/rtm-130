package com.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SplitOperationDTO {

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    @NotNull(message = "拆零数量不能为空")
    @Min(value = 1, message = "拆零数量最少为1")
    private Integer splitQuantity;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @Size(max = 100, message = "操作人长度不能超过100")
    private String operator;
}
