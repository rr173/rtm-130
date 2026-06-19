package com.pharmacy.repository;

import com.pharmacy.entity.MonitoringPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringPointRepository extends JpaRepository<MonitoringPoint, Long> {

    Optional<MonitoringPoint> findByPointCode(String pointCode);

    List<MonitoringPoint> findByEnabledTrue();

    @Query("SELECT DISTINCT mp FROM MonitoringPoint mp JOIN mp.boundBatchNos bn WHERE bn = :batchNo")
    List<MonitoringPoint> findByBoundBatchNo(@Param("batchNo") String batchNo);

    boolean existsByPointCode(String pointCode);
}
