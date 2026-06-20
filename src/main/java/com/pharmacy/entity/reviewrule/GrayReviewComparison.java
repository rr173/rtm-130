package com.pharmacy.entity.reviewrule;

import com.pharmacy.enums.ReviewResultType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gray_review_comparison", indexes = {
    @Index(name = "idx_gray_release", columnList = "grayReleaseId"),
    @Index(name = "idx_prescription_no", columnList = "prescriptionNo"),
    @Index(name = "idx_is_consistent", columnList = "isConsistent")
})
public class GrayReviewComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long grayReleaseId;

    @Column(nullable = false, length = 50)
    private String prescriptionNo;

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewResultType oldRuleResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewResultType newRuleResult;

    @Column(nullable = false)
    private Boolean isConsistent;

    @Column(length = 2000)
    private String oldRuleDetails;

    @Column(length = 2000)
    private String newRuleDetails;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
