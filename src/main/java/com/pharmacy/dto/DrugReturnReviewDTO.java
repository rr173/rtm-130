package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DrugReturnReviewDTO {

    @NotBlank(message = "退药单号不能为空")
    private String returnNo;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @NotBlank(message = "审核人不能为空")
    private String reviewedBy;

    private String reviewComment;

    private String lossReason;
}
