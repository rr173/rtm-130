package com.pharmacy.repository;

import com.pharmacy.entity.PharmacistReviewRecord;
import com.pharmacy.enums.PharmacistReviewConclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PharmacistReviewRecordRepository extends JpaRepository<PharmacistReviewRecord, Long> {

    List<PharmacistReviewRecord> findByPrescriptionIdOrderByCreatedAtDesc(Long prescriptionId);

    List<PharmacistReviewRecord> findByPharmacistIdOrderByReviewedAtDesc(String pharmacistId);

    @Query("SELECT r FROM PharmacistReviewRecord r WHERE r.pharmacistId = :pharmacistId " +
           "AND r.reviewedAt BETWEEN :startTime AND :endTime ORDER BY r.reviewedAt DESC")
    List<PharmacistReviewRecord> findByPharmacistIdAndReviewedAtBetween(
            @Param("pharmacistId") String pharmacistId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT r.pharmacistId, COUNT(r) FROM PharmacistReviewRecord r " +
           "WHERE r.reviewedAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.pharmacistId ORDER BY COUNT(r) DESC")
    List<Object[]> countByPharmacistAndPeriod(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT r FROM PharmacistReviewRecord r WHERE r.reviewedAt BETWEEN :startTime AND :endTime")
    List<PharmacistReviewRecord> findAllByReviewedAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    long countByPharmacistIdAndConclusionAndReviewedAtBetween(
            String pharmacistId, PharmacistReviewConclusion conclusion,
            LocalDateTime startTime, LocalDateTime endTime);

    long countByPharmacistIdAndIsTimeoutAndReviewedAtBetween(
            String pharmacistId, Boolean isTimeout,
            LocalDateTime startTime, LocalDateTime endTime);
}
