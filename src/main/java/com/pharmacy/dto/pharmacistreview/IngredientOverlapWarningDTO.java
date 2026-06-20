package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientOverlapWarningDTO {

    private Boolean hasOverlap;
    private List<OverlapDetailDTO> overlapDetails;
    private String warningMessage;
}
