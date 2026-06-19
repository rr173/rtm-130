package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temp_humidity_reading", indexes = {
    @Index(name = "idx_reading_point_code", columnList = "pointCode"),
    @Index(name = "idx_reading_collect_time", columnList = "collectTime"),
    @Index(name = "idx_reading_point_time", columnList = "pointCode, collectTime")
})
public class TempHumidityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String pointCode;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal humidity;

    @Column(nullable = false)
    private LocalDateTime collectTime;

    @Column(nullable = false)
    private Boolean tempOutOfRange = false;

    @Column(nullable = false)
    private Boolean humidityOutOfRange = false;

    @Column(length = 500)
    private String remark;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
