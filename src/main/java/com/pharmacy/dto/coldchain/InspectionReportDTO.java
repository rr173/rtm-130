package com.pharmacy.dto.coldchain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InspectionReportDTO {

    private Long id;
    private String inspectionType;
    private LocalDateTime inspectionTime;
    private Integer totalPoints;
    private Integer normalPoints;
    private Integer abnormalPoints;
    private String summary;
    private LocalDateTime createdAt;
    private List<InspectionReportItemDTO> items;
}
