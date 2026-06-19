package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "drug_return_item_batch", indexes = {
    @Index(name = "idx_return_batch_item_id", columnList = "drugReturnItemId"),
    @Index(name = "idx_return_batch_batch_id", columnList = "batchId"),
    @Index(name = "idx_return_batch_batch_no", columnList = "batchNo"),
    @Index(name = "idx_return_pending", columnList = "pending")
})
public class DrugReturnItemBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_return_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DrugReturnItem drugReturnItem;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 100)
    private String batchNo;

    @Column(nullable = false)
    private Integer returnQuantity;

    @Column(nullable = false)
    private Integer originalDispensedQuantity;

    @Column(nullable = false)
    private Boolean fromSplit = false;

    @Column(nullable = false)
    private Boolean pending = false;

    @Column(length = 500)
    private String pendingReason;
}
