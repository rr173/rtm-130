package com.pharmacy.entity;

import com.pharmacy.enums.ConsultationConclusion;
import com.pharmacy.enums.ConsultationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "consultation_record", indexes = {
    @Index(name = "idx_prescription_id", columnList = "prescription_id"),
    @Index(name = "idx_initiator_pharmacist_id", columnList = "initiatorPharmacistId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class ConsultationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Prescription prescription;

    @Column(nullable = false, length = 50)
    private String prescriptionNo;

    @Column(nullable = false, length = 50)
    private String initiatorPharmacistId;

    @Column(length = 100)
    private String initiatorPharmacistName;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsultationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConsultationConclusion finalConclusion;

    @Column(length = 2000)
    private String summaryComments;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Long totalDurationSeconds;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationOpinion> opinions = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void addOpinion(ConsultationOpinion opinion) {
        opinions.add(opinion);
        opinion.setConsultation(this);
    }
}
