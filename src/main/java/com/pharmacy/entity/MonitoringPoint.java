package com.pharmacy.entity;

import com.pharmacy.enums.MonitoringPointType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "monitoring_point", indexes = {
    @Index(name = "idx_mp_code", columnList = "pointCode", unique = true),
    @Index(name = "idx_mp_type", columnList = "pointType"),
    @Index(name = "idx_mp_status", columnList = "enabled")
})
public class MonitoringPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String pointCode;

    @Column(nullable = false, length = 200)
    private String pointName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MonitoringPointType pointType;

    @Column(length = 500)
    private String locationDescription;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal tempMin;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal tempMax;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal humidityMax;

    @Column(nullable = false)
    private Boolean enabled = true;

    @ElementCollection
    @CollectionTable(name = "monitoring_point_batch", joinColumns = @JoinColumn(name = "point_id"))
    @Column(name = "batch_no", length = 100)
    private List<String> boundBatchNos = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public boolean isTemperatureInRange(BigDecimal temperature) {
        return temperature.compareTo(tempMin) >= 0 && temperature.compareTo(tempMax) <= 0;
    }

    public boolean isHumidityInRange(BigDecimal humidity) {
        return humidity.compareTo(humidityMin) >= 0 && humidity.compareTo(humidityMax) <= 0;
    }
}
