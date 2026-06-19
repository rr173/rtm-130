package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BatchRecallDTO {

    @NotBlank(message = "批号不能为空")
    @Size(max = 100, message = "批号长度不能超过100")
    private String batchNo;

    @Size(max = 500, message = "召回原因长度不能超过500")
    private String reason;

    @Size(max = 100, message = "操作人长度不能超过100")
    private String operator;
}
