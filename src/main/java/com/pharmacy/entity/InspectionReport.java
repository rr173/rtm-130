package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "inspection_report", indexes = {
    @Index(name = "idx_inspection_created", columnList = "createdAt"),
    @Index(name = "idx_inspection_type", columnList = "inspectionType")
})
public class InspectionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String inspectionType = "DAILY_AUTO";

    @Column(nullable = false)
    private LocalDateTime inspectionTime;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Integer normalPoints;

    @Column(nullable = false)
    private Integer abnormalPoints;

    @Column(length = 2000)
    private String summary;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InspectionReportItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void addItem(InspectionReportItem item) {
        items.add(item);
        item.setReport(this);
    }
}
