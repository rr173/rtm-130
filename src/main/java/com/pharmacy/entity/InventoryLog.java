package com.pharmacy.entity;

import com.pharmacy.enums.InventoryLogType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_log", indexes = {
    @Index(name = "idx_drug_code", columnList = "drugCode"),
    @Index(name = "idx_log_type", columnList = "logType"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryLogType logType;

    @Column(nullable = false)
    private Integer quantity;

    private Integer beforeAvailableStock;

    private Integer afterAvailableStock;

    private Integer beforePreoccupiedStock;

    private Integer afterPreoccupiedStock;

    @Column(length = 50)
    private String prescriptionNo;

    @Column(length = 500)
    private String remark;

    @Column(length = 100)
    private String operator;

    @Column(length = 100)
    private String batchNo;

    @Column(length = 2000)
    private String batchDetails;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
