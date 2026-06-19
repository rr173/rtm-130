package com.pharmacy.entity;

import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.enums.QueueItemStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispense_queue_item", indexes = {
    @Index(name = "idx_queue_prescription_no", columnList = "prescriptionNo"),
    @Index(name = "idx_queue_status", columnList = "status"),
    @Index(name = "idx_queue_enqueue_time", columnList = "enqueueTime")
})
public class DispenseQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String prescriptionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrescriptionType prescriptionType;

    @Column(nullable = false)
    private LocalDateTime enqueueTime;

    @Column(nullable = false)
    private Integer sortPriority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueItemStatus status;

    @Column(length = 20)
    private String windowNo;

    private LocalDateTime claimTime;

    private LocalDateTime completeTime;

    private Integer returnCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
