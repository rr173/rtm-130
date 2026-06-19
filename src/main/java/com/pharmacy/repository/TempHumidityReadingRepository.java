package com.pharmacy.repository;

import com.pharmacy.entity.TempHumidityReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TempHumidityReadingRepository extends JpaRepository<TempHumidityReading, Long> {

    List<TempHumidityReading> findTopNByPointCodeOrderByCollectTimeDesc(String pointCode, int n);

    @Query("SELECT r FROM TempHumidityReading r WHERE r.pointCode = :pointCode " +
           "AND r.collectTime BETWEEN :startTime AND :endTime ORDER BY r.collectTime ASC")
    List<TempHumidityReading> findByPointCodeAndCollectTimeBetween(
            @Param("pointCode") String pointCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    Optional<TempHumidityReading> findTopByPointCodeOrderByCollectTimeDesc(String pointCode);

    @Query("SELECT MAX(r.temperature) FROM TempHumidityReading r WHERE r.pointCode = :pointCode " +
           "AND r.collectTime BETWEEN :startTime AND :endTime")
    BigDecimal findMaxTemperatureByPointCodeAndTimeRange(
            @Param("pointCode") String pointCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT MIN(r.temperature) FROM TempHumidityReading r WHERE r.pointCode = :pointCode " +
           "AND r.collectTime BETWEEN :startTime AND :endTime")
    BigDecimal findMinTemperatureByPointCodeAndTimeRange(
            @Param("pointCode") String pointCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT MAX(r.humidity) FROM TempHumidityReading r WHERE r.pointCode = :pointCode " +
           "AND r.collectTime BETWEEN :startTime AND :endTime")
    BigDecimal findMaxHumidityByPointCodeAndTimeRange(
            @Param("pointCode") String pointCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT MIN(r.humidity) FROM TempHumidityReading r WHERE r.pointCode = :pointCode " +
           "AND r.collectTime BETWEEN :startTime AND :endTime")
    BigDecimal findMinHumidityByPointCodeAndTimeRange(
            @Param("pointCode") String pointCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
