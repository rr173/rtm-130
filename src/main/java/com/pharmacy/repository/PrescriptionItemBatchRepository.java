package com.pharmacy.repository;

import com.pharmacy.entity.PrescriptionItemBatch;
import com.pharmacy.enums.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemBatchRepository extends JpaRepository<PrescriptionItemBatch, Long> {

    List<PrescriptionItemBatch> findByPrescriptionItemId(Long prescriptionItemId);

    List<PrescriptionItemBatch> findByBatchId(Long batchId);

    List<PrescriptionItemBatch> findByBatchNo(String batchNo);

    @Query("SELECT pib FROM PrescriptionItemBatch pib " +
           "JOIN pib.prescriptionItem pi " +
           "JOIN pi.prescription p " +
           "WHERE pib.batchNo = :batchNo AND p.status = :status")
    List<PrescriptionItemBatch> findByBatchNoAndPrescriptionStatus(
            @Param("batchNo") String batchNo,
            @Param("status") PrescriptionStatus status);

    @Query("SELECT pib FROM PrescriptionItemBatch pib " +
           "JOIN pib.prescriptionItem pi " +
           "JOIN pi.prescription p " +
           "WHERE p.patientId = :patientId AND p.status = :status " +
           "ORDER BY p.dispensedAt DESC")
    List<PrescriptionItemBatch> findByPatientIdAndPrescriptionStatus(
            @Param("patientId") String patientId,
            @Param("status") PrescriptionStatus status);
}
