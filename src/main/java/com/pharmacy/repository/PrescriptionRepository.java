package com.pharmacy.repository;

import com.pharmacy.entity.Prescription;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByPrescriptionNo(String prescriptionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Prescription p WHERE p.prescriptionNo = :prescriptionNo")
    Optional<Prescription> findByPrescriptionNoWithLock(@Param("prescriptionNo") String prescriptionNo);

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(String patientId);

    Page<Prescription> findByStatus(PrescriptionStatus status, Pageable pageable);

    List<Prescription> findByStatus(PrescriptionStatus status);

    Page<Prescription> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Prescription p WHERE p.patientId = :patientId " +
           "AND p.type = :type AND p.status NOT IN ('CANCELLED', 'REVIEW_BLOCKED') " +
           "AND p.createdAt >= :startTime ORDER BY p.createdAt DESC")
    List<Prescription> findRecentPrescriptionsForPatient(
            @Param("patientId") String patientId,
            @Param("type") PrescriptionType type,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT p FROM Prescription p WHERE p.patientId = :patientId " +
           "AND p.status NOT IN ('CANCELLED', 'REVIEW_BLOCKED') " +
           "AND p.createdAt >= :startTime ORDER BY p.createdAt DESC")
    List<Prescription> findRecentPrescriptionsForPatient(
            @Param("patientId") String patientId,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT p FROM Prescription p WHERE p.status = :status " +
           "AND p.createdAt BETWEEN :startTime AND :endTime ORDER BY p.createdAt DESC")
    List<Prescription> findByStatusAndDateRange(
            @Param("status") PrescriptionStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    boolean existsByPrescriptionNo(String prescriptionNo);
}
