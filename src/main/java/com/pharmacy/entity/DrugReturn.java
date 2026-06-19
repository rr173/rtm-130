package com.pharmacy.entity;

import com.pharmacy.enums.DrugReturnStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "drug_return", indexes = {
    @Index(name = "idx_return_no", columnList = "returnNo", unique = true),
    @Index(name = "idx_return_prescription_no", columnList = "prescriptionNo"),
    @Index(name = "idx_return_patient_id", columnList = "patientId"),
    @Index(name = "idx_return_status", columnList = "status"),
    @Index(name = "idx_return_created_at", columnList = "createdAt")
})
public class DrugReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String returnNo;

    @Column(nullable = false, length = 50)
    private String prescriptionNo;

    @Column(nullable = false, length = 50)
    private String patientId;

    @Column(length = 100)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DrugReturnStatus status;

    @Column(length = 500)
    private String returnReason;

    @Column(length = 50)
    private String appliedBy;

    private LocalDateTime appliedAt;

    @Column(length = 50)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String reviewComment;

    @Column(length = 500)
    private String lossReason;

    @OneToMany(mappedBy = "drugReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrugReturnItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void addItem(DrugReturnItem item) {
        items.add(item);
        item.setDrugReturn(this);
    }
}
