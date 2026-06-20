package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRecentDrugDTO {

    private String prescriptionNo;
    private LocalDateTime dispensedAt;
    private List<RecentDrugItemDTO> drugs;
}
