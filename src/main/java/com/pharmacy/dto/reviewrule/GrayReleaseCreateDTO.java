package com.pharmacy.dto.reviewrule;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GrayReleaseCreateDTO {

    @NotNull(message = "新配置版本号不能为空")
    private Integer newConfigVersion;

    @NotEmpty(message = "灰度科室列表不能为空")
    private List<String> departments;

    private String createdBy;

    private String remark;
}
