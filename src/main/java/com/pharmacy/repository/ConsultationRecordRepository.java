package com.pharmacy.repository;

import com.pharmacy.entity.ConsultationRecord;
import com.pharmacy.enums.ConsultationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, Long> {

    Optional<ConsultationRecord> findByPrescriptionId(Long prescriptionId);

    Optional<ConsultationRecord> findByPrescriptionNo(String prescriptionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConsultationRecord c WHERE c.id = :id")
    Optional<ConsultationRecord> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConsultationRecord c WHERE c.prescriptionNo = :prescriptionNo")
    Optional<ConsultationRecord> findByPrescriptionNoWithLock(@Param("prescriptionNo") String prescriptionNo);

    List<ConsultationRecord> findByInitiatorPharmacistIdOrderByCreatedAtDesc(String initiatorPharmacistId);

    List<ConsultationRecord> findByStatusOrderByCreatedAtDesc(ConsultationStatus status);

    @Query("SELECT c FROM ConsultationRecord c WHERE c.status = :status AND c.createdAt BETWEEN :startTime AND :endTime ORDER BY c.createdAt DESC")
    List<ConsultationRecord> findByStatusAndDateRange(
            @Param("status") ConsultationStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT c FROM ConsultationRecord c WHERE c.createdAt BETWEEN :startTime AND :endTime ORDER BY c.createdAt DESC")
    List<ConsultationRecord> findByDateRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(c) FROM ConsultationRecord c WHERE c.createdAt BETWEEN :startTime AND :endTime")
    long countByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT c FROM ConsultationRecord c JOIN c.opinions o WHERE o.pharmacistId = :pharmacistId ORDER BY c.createdAt DESC")
    List<ConsultationRecord> findByParticipatingPharmacistId(@Param("pharmacistId") String pharmacistId);

    @Query("SELECT c FROM ConsultationRecord c JOIN c.opinions o WHERE o.pharmacistId = :pharmacistId AND c.createdAt BETWEEN :startTime AND :endTime ORDER BY c.createdAt DESC")
    List<ConsultationRecord> findByParticipatingPharmacistIdAndDateRange(
            @Param("pharmacistId") String pharmacistId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    boolean existsByPrescriptionNoAndStatus(String prescriptionNo, ConsultationStatus status);
}
