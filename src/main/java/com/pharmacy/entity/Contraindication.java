package com.pharmacy.entity;

import com.pharmacy.enums.ContraindicationLevel;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contraindication", indexes = {
    @Index(name = "idx_drug_pair", columnList = "drugACode, drugBCode", unique = true)
})
public class Contraindication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String drugACode;

    @Column(nullable = false, length = 50)
    private String drugBCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContraindicationLevel level;

    @Column(nullable = false, length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
