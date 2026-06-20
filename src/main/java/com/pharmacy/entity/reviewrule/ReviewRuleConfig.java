package com.pharmacy.entity.reviewrule;

import com.pharmacy.enums.ReviewResultType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_rule_config")
public class ReviewRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer version;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxSingleDoseMultiplier;

    @Column(nullable = false)
    private Integer duplicateMedicationWindowHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewResultType severeContraindicationAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewResultType moderateContraindicationAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewResultType mildContraindicationAction;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static ReviewRuleConfig defaultConfig() {
        ReviewRuleConfig config = new ReviewRuleConfig();
        config.setVersion(1);
        config.setMaxSingleDoseMultiplier(BigDecimal.ONE);
        config.setDuplicateMedicationWindowHours(24);
        config.setSevereContraindicationAction(ReviewResultType.BLOCKED);
        config.setModerateContraindicationAction(ReviewResultType.WARNING);
        config.setMildContraindicationAction(ReviewResultType.WARNING);
        config.setDescription("系统默认配置");
        config.setCreatedBy("system");
        return config;
    }
}
