package com.pharmacy.entity;

import com.pharmacy.enums.DispenseChannel;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispense_record", indexes = {
    @Index(name = "idx_dr_window_no", columnList = "windowNo"),
    @Index(name = "idx_dr_prescription_no", columnList = "prescriptionNo"),
    @Index(name = "idx_dr_start_time", columnList = "startTime"),
    @Index(name = "idx_dr_channel", columnList = "channel")
})
public class DispenseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String windowNo;

    @Column(nullable = false, length = 50)
    private String prescriptionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispenseChannel channel;

    @Column(length = 50)
    private String pharmacistId;

    @Column(length = 100)
    private String pharmacistName;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Long durationSeconds;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
