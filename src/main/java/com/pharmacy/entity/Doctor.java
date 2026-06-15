package com.pharmacy.entity;

import com.pharmacy.enums.PrescriptionType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "doctor", indexes = {
    @Index(name = "idx_doctor_id", columnList = "doctorId", unique = true)
})
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String doctorId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String department;

    @ElementCollection
    @CollectionTable(name = "doctor_prescription_permission",
        joinColumns = @JoinColumn(name = "doctor_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "prescription_type", length = 30)
    private List<PrescriptionType> allowedPrescriptionTypes = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean canPrescribe(PrescriptionType type) {
        return allowedPrescriptionTypes.contains(type);
    }
}
