package com.pharmacy.dto.pharmacistreview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRankingDTO {

    private String rankingType;
    private List<PharmacistPerformanceDTO> rankings;
}
