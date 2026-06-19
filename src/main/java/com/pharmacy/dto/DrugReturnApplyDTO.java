package com.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DrugReturnApplyDTO {

    @NotBlank(message = "处方号不能为空")
    private String prescriptionNo;

    @NotBlank(message = "退药原因不能为空")
    private String returnReason;

    @NotBlank(message = "申请人不能为空")
    private String appliedBy;

    @NotEmpty(message = "退药明细不能为空")
    @Valid
    private List<DrugReturnItemApplyDTO> items;
}
