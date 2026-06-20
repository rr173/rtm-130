package com.pharmacy.entity;

import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.enums.ReviewResultType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "prescription", indexes = {
    @Index(name = "idx_prescription_no", columnList = "prescriptionNo", unique = true),
    @Index(name = "idx_patient_id", columnList = "patientId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String prescriptionNo;

    @Column(nullable = false, length = 50)
    private String patientId;

    @Column(length = 100)
    private String patientName;

    @Column(length = 20)
    private String diagnosisCode;

    @Column(length = 200)
    private String diagnosisName;

    @Column(nullable = false, length = 50)
    private String doctorId;

    @Column(length = 100)
    private String doctorName;

    @Column(length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrescriptionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrescriptionStatus status;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewRecord> reviewRecords = new ArrayList<>();

    @Column(length = 1000)
    private String reviewComments;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewResultType reviewResult;

    @Column(length = 1000)
    private String lackDrugDetails;

    @Column(length = 50)
    private String dispensedBy;

    private LocalDateTime dispensedAt;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancelReason;

    @Column(length = 50)
    private String claimedByPharmacistId;

    @Column(length = 100)
    private String claimedByPharmacistName;

    private LocalDateTime claimedAt;

    private LocalDateTime reviewDeadline;

    @Column(length = 500)
    private String pharmacistReturnReason;

    @Column(length = 500)
    private String pharmacistAttentionReason;

    @Column(length = 2000)
    private String pharmacistReviewComments;

    @Column(length = 50)
    private String reviewedByPharmacistId;

    @Column(length = 100)
    private String reviewedByPharmacistName;

    private LocalDateTime pharmacistReviewedAt;

    private Boolean isKeyAttention = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }

    public void addReviewRecord(ReviewRecord record) {
        reviewRecords.add(record);
        record.setPrescription(this);
    }
}
