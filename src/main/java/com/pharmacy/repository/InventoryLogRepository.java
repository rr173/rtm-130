package com.pharmacy.repository;

import com.pharmacy.entity.InventoryLog;
import com.pharmacy.enums.InventoryLogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    List<InventoryLog> findByDrugCodeOrderByCreatedAtDesc(String drugCode);

    List<InventoryLog> findByLogTypeOrderByCreatedAtDesc(InventoryLogType logType);

    List<InventoryLog> findByDrugCodeAndCreatedAtBetweenOrderByCreatedAtDesc(
            String drugCode, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT il FROM InventoryLog il WHERE il.logType = :logType " +
           "AND il.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY il.createdAt DESC")
    List<InventoryLog> findByLogTypeAndDateRange(
            @Param("logType") InventoryLogType logType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT il.drugCode, il.drugName, SUM(il.quantity) as totalQuantity " +
           "FROM InventoryLog il WHERE il.logType = :logType " +
           "AND il.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY il.drugCode, il.drugName ORDER BY totalQuantity DESC")
    List<Object[]> sumQuantityByDrugAndDateRange(
            @Param("logType") InventoryLogType logType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<InventoryLog> findByPrescriptionNoOrderByCreatedAtDesc(String prescriptionNo);
}
