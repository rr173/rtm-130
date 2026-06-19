package com.pharmacy.entity;

import com.pharmacy.enums.InspectionAbnormalType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "inspection_report_item", indexes = {
    @Index(name = "idx_item_report_id", columnList = "report_id"),
    @Index(name = "idx_item_point_code", columnList = "pointCode")
})
public class InspectionReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private InspectionReport report;

    @Column(nullable = false, length = 50)
    private String pointCode;

    @Column(nullable = false, length = 200)
    private String pointName;

    @Column(nullable = false)
    private Boolean isNormal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inspection_item_abnormal_types", joinColumns = @JoinColumn(name = "item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "abnormal_type", length = 50)
    private List<InspectionAbnormalType> abnormalTypes = new ArrayList<>();

    private Integer expectedReadingCount;

    private Integer actualReadingCount;

    private BigDecimal frequencyRatio;

    private BigDecimal firstTemperature;

    private BigDecimal lastTemperature;

    private BigDecimal temperatureDifference;

    @Column(length = 2000)
    private String remark;
}
