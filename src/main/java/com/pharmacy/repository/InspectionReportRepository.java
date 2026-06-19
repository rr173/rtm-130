package com.pharmacy.repository;

import com.pharmacy.entity.InspectionReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InspectionReportRepository extends JpaRepository<InspectionReport, Long> {

    List<InspectionReport> findAllByOrderByCreatedAtDesc();

    List<InspectionReport> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);

    List<InspectionReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
