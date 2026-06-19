package com.pharmacy.entity;

import com.pharmacy.enums.DisposalDecision;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cold_chain_disposal", indexes = {
    @Index(name = "idx_disposal_batch_no", columnList = "batchNo"),
    @Index(name = "idx_disposal_alert_id", columnList = "alertId"),
    @Index(name = "idx_disposal_decision", columnList = "decision"),
    @Index(name = "idx_disposal_created", columnList = "createdAt")
})
public class ColdChainDisposalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String pointCode;

    @Column(nullable = false)
    private Long alertId;

    @Column(nullable = false, length = 100)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Column(nullable = false, length = 100)
    private String batchNo;

    @Column(nullable = false)
    private Integer affectedQuantity;

    @Column(nullable = false)
    private LocalDateTime interruptionStartTime;

    private LocalDateTime interruptionEndTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal interruptionDurationMinutes;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxToleranceMinutes;

    @Column(precision = 5, scale = 2)
    private BigDecimal maxRecordedTemperature;

    @Column(precision = 5, scale = 2)
    private BigDecimal minRecordedTemperature;

    @Column(precision = 5, scale = 2)
    private BigDecimal drugExtremeTemperature;

    @Column(nullable = false)
    private Boolean withinTolerance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisposalDecision decision;

    @Column(length = 100)
    private String disposedBy;

    @Column(length = 1000)
    private String disposalRemark;

    @Column(nullable = false)
    private LocalDateTime disposedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
