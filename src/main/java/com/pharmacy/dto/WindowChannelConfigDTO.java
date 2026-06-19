package com.pharmacy.dto;

import com.pharmacy.enums.DispenseChannel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowChannelConfigDTO {

    @NotNull(message = "窗口编号不能为空")
    private String windowNo;

    @NotNull(message = "服务通道不能为空")
    private DispenseChannel serviceChannel;
}
