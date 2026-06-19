package com.pharmacy.repository;

import com.pharmacy.entity.DrugSplitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugSplitRecordRepository extends JpaRepository<DrugSplitRecord, Long> {

    List<DrugSplitRecord> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    List<DrugSplitRecord> findByDrugCodeOrderByCreatedAtDesc(String drugCode);

    Optional<DrugSplitRecord> findByBatchIdAndActiveTrue(Long batchId);

    @Query("SELECT s FROM DrugSplitRecord s WHERE s.batchId = :batchId AND s.active = true AND s.remainingSplitQuantity > 0")
    List<DrugSplitRecord> findActiveSplitRecordsByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT s FROM DrugSplitRecord s WHERE s.drugCode = :drugCode AND s.active = true AND s.remainingSplitQuantity > 0 ORDER BY s.createdAt ASC")
    List<DrugSplitRecord> findActiveSplitRecordsByDrugCode(@Param("drugCode") String drugCode);

    List<DrugSplitRecord> findByPrescriptionNo(String prescriptionNo);
}
