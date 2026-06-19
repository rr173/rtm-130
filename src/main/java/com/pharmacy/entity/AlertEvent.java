package com.pharmacy.entity;

import com.pharmacy.enums.AlertLevel;
import com.pharmacy.enums.AlertStatus;
import com.pharmacy.enums.AlertType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_event", indexes = {
    @Index(name = "idx_alert_point_code", columnList = "pointCode"),
    @Index(name = "idx_alert_level", columnList = "alertLevel"),
    @Index(name = "idx_alert_status", columnList = "alertStatus"),
    @Index(name = "idx_alert_parent_id", columnList = "parentAlertId"),
    @Index(name = "idx_alert_created", columnList = "createdAt")
})
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String pointCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertLevel alertLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType alertType = AlertType.TEMPERATURE_HUMIDITY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus alertStatus;

    @Column(precision = 5, scale = 2)
    private BigDecimal triggerTemperature;

    @Column(precision = 5, scale = 2)
    private BigDecimal triggerHumidity;

    @Column(precision = 5, scale = 2)
    private BigDecimal tempLowerLimit;

    @Column(precision = 5, scale = 2)
    private BigDecimal tempUpperLimit;

    @Column(precision = 5, scale = 2)
    private BigDecimal humidityLowerLimit;

    @Column(precision = 5, scale = 2)
    private BigDecimal humidityUpperLimit;

    @Column(nullable = false)
    private LocalDateTime firstTriggerTime;

    private LocalDateTime lastTriggerTime;

    private LocalDateTime resolvedTime;

    private Long parentAlertId;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String resolvedBy;

    @Column(length = 500)
    private String resolvedRemark;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
