package com.pharmacy.repository;

import com.pharmacy.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    List<ReviewRecord> findByPrescriptionIdOrderByCreatedAtDesc(Long prescriptionId);
}
