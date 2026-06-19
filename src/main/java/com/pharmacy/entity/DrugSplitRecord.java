package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "drug_split_record", indexes = {
    @Index(name = "idx_split_batch_id", columnList = "batchId"),
    @Index(name = "idx_split_drug_code", columnList = "drugCode"),
    @Index(name = "idx_split_prescription_no", columnList = "prescriptionNo")
})
public class DrugSplitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Column(nullable = false, length = 100)
    private String batchNo;

    @Column(nullable = false)
    private Integer packageQuantity;

    @Column(nullable = false)
    private Integer splitUnit;

    @Column(nullable = false)
    private Integer splitQuantity;

    @Column(nullable = false)
    private Integer dispensedSplitQuantity;

    @Column(nullable = false)
    private Integer remainingSplitQuantity;

    @Column(length = 50)
    private String prescriptionNo;

    @Column(length = 100)
    private String operator;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;
}
