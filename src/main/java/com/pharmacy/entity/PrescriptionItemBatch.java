package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "prescription_item_batch", indexes = {
    @Index(name = "idx_prescription_item_item_id", columnList = "prescriptionItemId"),
    @Index(name = "idx_prescription_item_batch_id", columnList = "batchId"),
    @Index(name = "idx_prescription_item_batch_no", columnList = "batchNo")
})
public class PrescriptionItemBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PrescriptionItem prescriptionItem;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 100)
    private String batchNo;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Boolean fromSplit = false;

    private Long splitRecordId;
}
