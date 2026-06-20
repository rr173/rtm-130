package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverlapDetailDTO {

    private String ingredient;
    private String currentDrugName;
    private String currentDrugCode;
    private List<String> recentDrugNames;
    private List<String> recentPrescriptionNos;
}
