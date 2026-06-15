package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DispenseConfirmDTO {

    @NotBlank(message = "处方编号不能为空")
    private String prescriptionNo;

    @Size(max = 50, message = "发药人长度不能超过50")
    private String dispensedBy;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
