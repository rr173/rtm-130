package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "drug_return_item", indexes = {
    @Index(name = "idx_return_item_return_id", columnList = "drugReturnId"),
    @Index(name = "idx_return_item_drug_code", columnList = "drugCode")
})
public class DrugReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_return_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DrugReturn drugReturn;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Column(nullable = false)
    private Long prescriptionItemId;

    @Column(nullable = false)
    private Integer returnQuantity;

    @Column(nullable = false)
    private Integer originalDispensedQuantity;

    @Column(length = 500)
    private String returnItemReason;

    @OneToMany(mappedBy = "drugReturnItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrugReturnItemBatch> batchAllocations = new ArrayList<>();

    public void addBatchAllocation(DrugReturnItemBatch allocation) {
        batchAllocations.add(allocation);
        allocation.setDrugReturnItem(this);
    }
}
