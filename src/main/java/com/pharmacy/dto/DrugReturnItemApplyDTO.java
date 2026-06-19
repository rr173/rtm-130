package com.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DrugReturnItemApplyDTO {

    @NotNull(message = "处方明细ID不能为空")
    private Long prescriptionItemId;

    @NotNull(message = "退药数量不能为空")
    @Min(value = 1, message = "退药数量必须大于0")
    private Integer returnQuantity;

    private String returnItemReason;
}
