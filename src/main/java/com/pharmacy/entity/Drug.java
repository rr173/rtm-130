package com.pharmacy.entity;

import com.pharmacy.enums.DrugCategory;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "drug", indexes = {
    @Index(name = "idx_drug_code", columnList = "drugCode", unique = true)
})
public class Drug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String specification;

    @Column(nullable = false, length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DrugCategory category;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxSingleDose;

    @Column(length = 50)
    private String maxSingleDoseUnit;

    @Column(nullable = false)
    private Integer availableStock = 0;

    @Column(nullable = false)
    private Integer preoccupiedStock = 0;

    @Column(length = 500)
    private String ingredient;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public int getActualStock() {
        return availableStock + preoccupiedStock;
    }
}
