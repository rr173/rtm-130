package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WindowClaimDTO {

    @NotBlank(message = "窗口编号不能为空")
    private String windowNo;

    private String pharmacistId;

    private String pharmacistName;
}
