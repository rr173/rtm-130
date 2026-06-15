package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PharmacistConfirmDTO {

    @NotBlank(message = "处方编号不能为空")
    private String prescriptionNo;

    @NotNull(message = "确认结果不能为空")
    private Boolean confirmed;

    @Size(max = 50, message = "药师姓名长度不能超过50")
    private String pharmacistName;

    @Size(max = 500, message = "审核意见长度不能超过500")
    private String comments;
}
