package com.pharmacy.dto.reviewrule;

import com.pharmacy.enums.GrayReleaseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrayReleaseConfigDTO {

    private Long id;
    private Integer newConfigVersion;
    private Integer baseConfigVersion;
    private GrayReleaseStatus status;
    private List<String> departments;
    private String createdBy;
    private String releasedBy;
    private String cancelledBy;
    private String remark;
    private LocalDateTime grayStartTime;
    private LocalDateTime fullReleaseTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
