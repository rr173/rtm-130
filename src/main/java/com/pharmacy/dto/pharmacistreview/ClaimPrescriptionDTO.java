package com.pharmacy.dto.pharmacistreview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimPrescriptionDTO {

    @NotBlank(message = "药师ID不能为空")
    private String pharmacistId;

    private String pharmacistName;
}
