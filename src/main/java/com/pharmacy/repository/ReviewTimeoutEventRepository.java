package com.pharmacy.repository;

import com.pharmacy.entity.ReviewTimeoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewTimeoutEventRepository extends JpaRepository<ReviewTimeoutEvent, Long> {

    List<ReviewTimeoutEvent> findByPrescriptionIdOrderByTimeoutAtDesc(Long prescriptionId);

    @Query("SELECT t FROM ReviewTimeoutEvent t WHERE t.timeoutAt BETWEEN :startTime AND :endTime ORDER BY t.timeoutAt DESC")
    List<ReviewTimeoutEvent> findByTimeoutAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    long countByPharmacistIdAndTimeoutAtBetween(String pharmacistId, LocalDateTime startTime, LocalDateTime endTime);
}
