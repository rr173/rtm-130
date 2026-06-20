package com.pharmacy.dto.consultation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ConsultationInitiateDTO {

    @NotBlank(message = "处方编号不能为空")
    private String prescriptionNo;

    @NotBlank(message = "发起药师ID不能为空")
    private String pharmacistId;

    private String pharmacistName;

    @NotBlank(message = "会诊原因不能为空")
    @Size(max = 1000, message = "会诊原因不能超过1000字")
    private String reason;

    @NotEmpty(message = "邀请药师列表不能为空")
    @Size(min = 1, max = 3, message = "邀请药师人数必须在1-3人之间")
    private List<String> invitedPharmacistIds;
}
