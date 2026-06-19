package com.pharmacy.repository;

import com.pharmacy.entity.AlertEvent;
import com.pharmacy.enums.AlertLevel;
import com.pharmacy.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    @Query("SELECT a FROM AlertEvent a WHERE a.pointCode = :pointCode AND a.alertStatus IN :statuses " +
           "AND a.parentAlertId IS NULL ORDER BY a.firstTriggerTime DESC")
    List<AlertEvent> findActiveRootAlertsByPointCode(
            @Param("pointCode") String pointCode,
            @Param("statuses") List<AlertStatus> statuses);

    @Query("SELECT a FROM AlertEvent a WHERE a.alertLevel = :alertLevel AND a.alertStatus IN :statuses " +
           "ORDER BY a.firstTriggerTime DESC")
    List<AlertEvent> findByLevelAndStatusIn(
            @Param("alertLevel") AlertLevel alertLevel,
            @Param("statuses") List<AlertStatus> statuses);

    @Query("SELECT a FROM AlertEvent a WHERE a.alertStatus IN :statuses ORDER BY a.firstTriggerTime DESC")
    List<AlertEvent> findByStatusIn(@Param("statuses") List<AlertStatus> statuses);

    List<AlertEvent> findByParentAlertId(Long parentAlertId);

    @Query("SELECT a FROM AlertEvent a WHERE a.alertStatus = :status " +
           "AND a.alertLevel = :level AND a.lastTriggerTime <= :thresholdTime")
    List<AlertEvent> findAlertsEligibleForUpgrade(
            @Param("status") AlertStatus status,
            @Param("level") AlertLevel level,
            @Param("thresholdTime") LocalDateTime thresholdTime);

    @Query("SELECT a FROM AlertEvent a WHERE a.pointCode = :pointCode AND a.alertStatus IN :statuses " +
           "AND a.firstTriggerTime BETWEEN :startTime AND :endTime ORDER BY a.firstTriggerTime ASC")
    List<AlertEvent> findByPointCodeAndStatusInAndTimeRange(
            @Param("pointCode") String pointCode,
            @Param("statuses") List<AlertStatus> statuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM AlertEvent a WHERE a.id = :alertId OR a.parentAlertId = :alertId ORDER BY a.firstTriggerTime ASC")
    List<AlertEvent> findAlertChain(@Param("alertId") Long alertId);
}
