package com.pharmacy.entity;

import com.pharmacy.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "drug_batch", indexes = {
    @Index(name = "idx_batch_drug_code", columnList = "drugCode"),
    @Index(name = "idx_batch_no", columnList = "batchNo"),
    @Index(name = "idx_drug_batch_no", columnList = "drugCode, batchNo", unique = true),
    @Index(name = "idx_expiry_date", columnList = "expiryDate"),
    @Index(name = "idx_batch_status", columnList = "status")
})
public class DrugBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Column(nullable = false, length = 100)
    private String batchNo;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private LocalDate productionDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer preoccupiedQuantity;

    @Column(nullable = false)
    private Integer splitQuantity;

    @Column(nullable = false)
    private Integer dispensedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status;

    @Column(nullable = false)
    private Boolean splitLocked = false;

    @Column(length = 100)
    private String splitLockedBy;

    private LocalDateTime splitLockedAt;

    @Column(length = 500)
    private String remark;

    @Column(length = 100)
    private String operator;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public int getRemainingQuantity() {
        return availableQuantity + preoccupiedQuantity;
    }

    public int getActualRemaining() {
        return totalQuantity - dispensedQuantity;
    }
}
