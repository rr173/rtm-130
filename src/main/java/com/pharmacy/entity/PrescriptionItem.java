package com.pharmacy.entity;

import com.pharmacy.enums.UsageType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "prescription_item")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(nullable = false, length = 50)
    private String drugCode;

    @Column(nullable = false, length = 200)
    private String drugName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal singleDose;

    @Column(length = 20)
    private String doseUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UsageType usage;

    @Column(nullable = false, length = 20)
    private String frequency;

    @Column(nullable = false)
    private Integer days;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(length = 200)
    private String dispensingNotes;

    private Boolean preoccupied = false;

    private Boolean dispensed = false;
}
