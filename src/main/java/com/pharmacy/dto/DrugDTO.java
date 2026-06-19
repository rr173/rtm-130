package com.pharmacy.dto;

import com.pharmacy.entity.Drug;
import com.pharmacy.enums.DrugCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugDTO {

    private Long id;
    private String drugCode;
    private String name;
    private String specification;
    private String unit;
    private DrugCategory category;
    private String categoryDescription;
    private BigDecimal maxSingleDose;
    private String maxSingleDoseUnit;
    private Integer availableStock;
    private Integer preoccupiedStock;
    private Integer actualStock;
    private String ingredient;
    private Boolean splittable;
    private String packageUnit;
    private Integer packageQuantity;
    private String splitUnit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DrugDTO fromEntity(Drug drug) {
        DrugDTO dto = new DrugDTO();
        dto.setId(drug.getId());
        dto.setDrugCode(drug.getDrugCode());
        dto.setName(drug.getName());
        dto.setSpecification(drug.getSpecification());
        dto.setUnit(drug.getUnit());
        dto.setCategory(drug.getCategory());
        dto.setCategoryDescription(drug.getCategory().getDescription());
        dto.setMaxSingleDose(drug.getMaxSingleDose());
        dto.setMaxSingleDoseUnit(drug.getMaxSingleDoseUnit());
        dto.setAvailableStock(drug.getAvailableStock());
        dto.setPreoccupiedStock(drug.getPreoccupiedStock());
        dto.setActualStock(drug.getActualStock());
        dto.setIngredient(drug.getIngredient());
        dto.setSplittable(drug.getSplittable());
        dto.setPackageUnit(drug.getPackageUnit());
        dto.setPackageQuantity(drug.getPackageQuantity());
        dto.setSplitUnit(drug.getSplitUnit());
        dto.setCreatedAt(drug.getCreatedAt());
        dto.setUpdatedAt(drug.getUpdatedAt());
        return dto;
    }
}
