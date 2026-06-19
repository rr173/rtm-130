package com.pharmacy.repository;

import com.pharmacy.entity.BatchInventoryLog;
import com.pharmacy.enums.BatchLogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchInventoryLogRepository extends JpaRepository<BatchInventoryLog, Long> {

    List<BatchInventoryLog> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    List<BatchInventoryLog> findByDrugCodeOrderByCreatedAtDesc(String drugCode);

    List<BatchInventoryLog> findByBatchNoOrderByCreatedAtDesc(String batchNo);

    List<BatchInventoryLog> findByPrescriptionNoOrderByCreatedAtDesc(String prescriptionNo);

    List<BatchInventoryLog> findByLogTypeOrderByCreatedAtDesc(BatchLogType logType);

    @Query("SELECT l FROM BatchInventoryLog l WHERE l.drugCode = :drugCode " +
           "AND l.createdAt BETWEEN :startTime AND :endTime ORDER BY l.createdAt DESC")
    List<BatchInventoryLog> findByDrugCodeAndDateRange(
            @Param("drugCode") String drugCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT l FROM BatchInventoryLog l WHERE l.logType = :logType " +
           "AND l.createdAt BETWEEN :startTime AND :endTime ORDER BY l.createdAt DESC")
    List<BatchInventoryLog> findByLogTypeAndDateRange(
            @Param("logType") BatchLogType logType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT l FROM BatchInventoryLog l WHERE l.prescriptionNo = :prescriptionNo " +
           "AND l.logType = :logType")
    List<BatchInventoryLog> findByPrescriptionNoAndLogType(
            @Param("prescriptionNo") String prescriptionNo,
            @Param("logType") BatchLogType logType);
}
