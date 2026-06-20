package com.pharmacy.dto.consultation;

import com.pharmacy.enums.ConsultationOpinionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsultationOpinionSubmitDTO {

    @NotBlank(message = "药师ID不能为空")
    private String pharmacistId;

    private String pharmacistName;

    @NotNull(message = "意见类型不能为空")
    private ConsultationOpinionType opinionType;

    private String reason;
}
