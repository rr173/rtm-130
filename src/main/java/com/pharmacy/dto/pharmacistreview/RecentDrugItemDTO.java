package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentDrugItemDTO {

    private String drugCode;
    private String drugName;
    private String specification;
    private String ingredient;
    private Integer totalQuantity;
    private String unit;
}
