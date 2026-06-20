package com.pharmacy.entity.reviewrule;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_rule_change_log")
public class ReviewRuleChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer configVersion;

    @Column(nullable = false, length = 100)
    private String fieldName;

    @Column(nullable = false, length = 100)
    private String fieldDisplayName;

    @Column(length = 500)
    private String oldValue;

    @Column(length = 500)
    private String newValue;

    @Column(nullable = false, length = 100)
    private String operator;

    @Column(length = 500)
    private String remark;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime effectiveAt;
}
