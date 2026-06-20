package com.pharmacy.dto.pharmacistreview;

import com.pharmacy.enums.PharmacistReviewConclusion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PharmacistReviewSubmitDTO {

    @NotBlank(message = "处方编号不能为空")
    private String prescriptionNo;

    @NotBlank(message = "药师ID不能为空")
    private String pharmacistId;

    private String pharmacistName;

    @NotNull(message = "审方结论不能为空")
    private PharmacistReviewConclusion conclusion;

    private String reviewComments;

    private String returnReason;

    private String attentionReason;
}
