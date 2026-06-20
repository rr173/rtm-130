package com.pharmacy.entity;

import com.pharmacy.enums.PharmacistReviewConclusion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pharmacist_review_record", indexes = {
    @Index(name = "idx_prescription_id", columnList = "prescription_id"),
    @Index(name = "idx_pharmacist_id", columnList = "pharmacistId"),
    @Index(name = "idx_reviewed_at", columnList = "reviewedAt"),
    @Index(name = "idx_conclusion", columnList = "conclusion")
})
public class PharmacistReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Prescription prescription;

    @Column(nullable = false, length = 50)
    private String pharmacistId;

    @Column(length = 100)
    private String pharmacistName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PharmacistReviewConclusion conclusion;

    @Column(length = 2000)
    private String reviewComments;

    @Column(length = 500)
    private String returnReason;

    @Column(length = 500)
    private String attentionReason;

    private LocalDateTime claimedAt;

    private LocalDateTime reviewedAt;

    private Long reviewDurationSeconds;

    private Boolean isTimeout = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
