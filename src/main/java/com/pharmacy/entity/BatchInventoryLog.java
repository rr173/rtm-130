package com.pharmacy.entity;

import com.pharmacy.enums.BatchLogType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "batch_inventory_log", indexes = {
    @Index(name = "idx_batch_log_batch_id", columnList = "batchId"),
    @Index(name = "idx_batch_log_drug_code", columnList = "drugCode"),
    @Index(name = "idx_batch_log_type", columnList = "logType"),
    @Index(name = "idx_batch_log_prescription_no", columnList = "prescriptionNo"),
    @Index(name = "idx_batch_log_created_at", columnList = "createdAt")
})
public class BatchInventoryLog {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchLogType logType;

    @Column(nullable = false)
    private Integer quantity;

    private Integer beforeAvailable;

    private Integer afterAvailable;

    private Integer beforePreoccupied;

    private Integer afterPreoccupied;

    private Integer beforeSplit;

    private Integer afterSplit;

    @Column(length = 50)
    private String prescriptionNo;

    @Column(length = 500)
    private String remark;

    @Column(length = 100)
    private String operator;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
