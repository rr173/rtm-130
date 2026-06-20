package com.pharmacy.entity.reviewrule;

import com.pharmacy.enums.GrayReleaseStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "gray_release_config")
public class GrayReleaseConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer newConfigVersion;

    @Column(nullable = false)
    private Integer baseConfigVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GrayReleaseStatus status;

    @Column(nullable = false, length = 500)
    private String departmentList;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String releasedBy;

    @Column(length = 100)
    private String cancelledBy;

    @Column(length = 500)
    private String remark;

    private LocalDateTime grayStartTime;

    private LocalDateTime fullReleaseTime;

    private LocalDateTime cancelTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    public List<String> getDepartments() {
        if (departmentList == null || departmentList.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(departmentList.split(","));
    }

    public void setDepartments(List<String> departments) {
        this.departmentList = String.join(",", departments);
    }
}
