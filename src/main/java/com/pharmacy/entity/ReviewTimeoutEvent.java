package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_timeout_event", indexes = {
    @Index(name = "idx_prescription_id", columnList = "prescription_id"),
    @Index(name = "idx_pharmacist_id", columnList = "pharmacistId"),
    @Index(name = "idx_timeout_at", columnList = "timeoutAt")
})
public class ReviewTimeoutEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(length = 50)
    private String pharmacistId;

    @Column(length = 100)
    private String pharmacistName;

    @Column(nullable = false)
    private Integer timeoutMinutes;

    private LocalDateTime claimedAt;

    private LocalDateTime timeoutAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
