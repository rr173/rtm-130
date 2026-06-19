package com.pharmacy.entity;

import com.pharmacy.enums.DispenseChannel;
import com.pharmacy.enums.DispensingWindowStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispensing_window", indexes = {
    @Index(name = "idx_window_no", columnList = "windowNo", unique = true),
    @Index(name = "idx_window_status", columnList = "status")
})
public class DispensingWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String windowNo;

    @Column(nullable = false, length = 50)
    private String windowName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispensingWindowStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispenseChannel serviceChannel = DispenseChannel.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DispenseChannel currentServingChannel;

    @Column(length = 50)
    private String currentPrescriptionNo;

    @Column(length = 50)
    private String currentPharmacistId;

    @Column(length = 100)
    private String currentPharmacistName;

    private LocalDateTime dispenseStartTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
