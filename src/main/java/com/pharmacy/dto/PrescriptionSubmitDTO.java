package com.pharmacy.dto;

import com.pharmacy.enums.PrescriptionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionSubmitDTO {

    @NotBlank(message = "处方编号不能为空")
    @Size(max = 50, message = "处方编号长度不能超过50")
    private String prescriptionNo;

    @NotBlank(message = "患者ID不能为空")
    @Size(max = 50, message = "患者ID长度不能超过50")
    private String patientId;

    @Size(max = 100, message = "患者姓名长度不能超过100")
    private String patientName;

    @Size(max = 20, message = "诊断编码长度不能超过20")
    private String diagnosisCode;

    @Size(max = 200, message = "诊断名称长度不能超过200")
    private String diagnosisName;

    @NotBlank(message = "开方医生ID不能为空")
    @Size(max = 50, message = "开方医生ID长度不能超过50")
    private String doctorId;

    @Size(max = 100, message = "开方医生姓名长度不能超过100")
    private String doctorName;

    @Size(max = 100, message = "开方科室长度不能超过100")
    private String department;

    @NotNull(message = "处方类型不能为空")
    private PrescriptionType type;

    @NotEmpty(message = "药品明细不能为空")
    @Size(min = 1, max = 50, message = "药品明细数量在1-50之间")
    @Valid
    private List<PrescriptionItemDTO> items;
}
