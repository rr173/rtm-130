package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelPrescriptionDTO {

    @NotBlank(message = "处方编号不能为空")
    private String prescriptionNo;

    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因长度不能超过500")
    private String reason;

    @Size(max = 50, message = "操作人长度不能超过50")
    private String operator;
}
