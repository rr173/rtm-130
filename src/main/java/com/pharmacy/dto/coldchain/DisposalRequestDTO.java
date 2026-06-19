package com.pharmacy.dto.coldchain;

import com.pharmacy.enums.DisposalDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisposalRequestDTO {

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    @NotNull(message = "告警ID不能为空")
    private Long alertId;

    @NotNull(message = "处置决定不能为空")
    private DisposalDecision decision;

    @NotBlank(message = "操作人不能为空")
    private String disposedBy;

    private String disposalRemark;
}
