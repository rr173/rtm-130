package com.pharmacy.entity;

import com.pharmacy.enums.ConsultationOpinionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "consultation_opinion", indexes = {
    @Index(name = "idx_consultation_id", columnList = "consultation_id"),
    @Index(name = "idx_pharmacist_id", columnList = "pharmacistId"),
    @Index(name = "idx_deadline", columnList = "deadline"),
    @Index(name = "idx_submitted_at", columnList = "submittedAt")
})
public class ConsultationOpinion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ConsultationRecord consultation;

    @Column(nullable = false, length = 50)
    private String pharmacistId;

    @Column(length = 100)
    private String pharmacistName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConsultationOpinionType opinionType;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private Boolean isPrimary = false;

    private LocalDateTime deadline;

    private LocalDateTime submittedAt;

    private Boolean isTimeout = false;

    private Boolean isAbstained = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
