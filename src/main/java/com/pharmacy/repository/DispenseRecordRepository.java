package com.pharmacy.repository;

import com.pharmacy.entity.DispenseRecord;
import com.pharmacy.enums.DispenseChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DispenseRecordRepository extends JpaRepository<DispenseRecord, Long> {

    List<DispenseRecord> findByWindowNoAndStartTimeBetweenOrderByStartTimeDesc(
            String windowNo, LocalDateTime startTime, LocalDateTime endTime);

    List<DispenseRecord> findByStartTimeBetweenOrderByStartTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    long countByWindowNoAndStartTimeBetween(String windowNo, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT AVG(dr.durationSeconds) FROM DispenseRecord dr WHERE dr.windowNo = :windowNo " +
           "AND dr.startTime BETWEEN :startTime AND :endTime")
    Double avgDurationSecondsByWindowAndDateRange(
            @Param("windowNo") String windowNo,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT dr.windowNo, COUNT(dr), AVG(dr.durationSeconds) FROM DispenseRecord dr " +
           "WHERE dr.startTime BETWEEN :startTime AND :endTime GROUP BY dr.windowNo")
    List<Object[]> statsByWindowAndDateRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(dr) FROM DispenseRecord dr WHERE dr.channel = :channel " +
           "AND dr.startTime BETWEEN :startTime AND :endTime")
    long countByChannelAndDateRange(
            @Param("channel") DispenseChannel channel,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT AVG(dr.durationSeconds) FROM DispenseRecord dr WHERE dr.channel = :channel " +
           "AND dr.startTime BETWEEN :startTime AND :endTime")
    Double avgDurationSecondsByChannelAndDateRange(
            @Param("channel") DispenseChannel channel,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT dr.channel, COUNT(dr), AVG(dr.durationSeconds) FROM DispenseRecord dr " +
           "WHERE dr.startTime BETWEEN :startTime AND :endTime GROUP BY dr.channel")
    List<Object[]> statsByChannelAndDateRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<DispenseRecord> findByPrescriptionNo(String prescriptionNo);
}
