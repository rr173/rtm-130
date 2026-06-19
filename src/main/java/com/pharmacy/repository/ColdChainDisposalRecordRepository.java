package com.pharmacy.repository;

import com.pharmacy.entity.ColdChainDisposalRecord;
import com.pharmacy.enums.DisposalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColdChainDisposalRecordRepository extends JpaRepository<ColdChainDisposalRecord, Long> {

    List<ColdChainDisposalRecord> findByBatchNo(String batchNo);

    List<ColdChainDisposalRecord> findByAlertId(Long alertId);

    List<ColdChainDisposalRecord> findByPointCode(String pointCode);

    @Query("SELECT d FROM ColdChainDisposalRecord d WHERE d.batchNo = :batchNo AND d.decision = :decision")
    List<ColdChainDisposalRecord> findByBatchNoAndDecision(
            @Param("batchNo") String batchNo,
            @Param("decision") DisposalDecision decision);

    boolean existsByBatchNoAndAlertId(String batchNo, Long alertId);
}
