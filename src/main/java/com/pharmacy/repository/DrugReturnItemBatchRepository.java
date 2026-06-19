package com.pharmacy.repository;

import com.pharmacy.entity.DrugReturnItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugReturnItemBatchRepository extends JpaRepository<DrugReturnItemBatch, Long> {

    List<DrugReturnItemBatch> findByBatchIdOrderByIdDesc(Long batchId);

    List<DrugReturnItemBatch> findByBatchNoOrderByIdDesc(String batchNo);

    @Query("SELECT b FROM DrugReturnItemBatch b " +
           "JOIN b.drugReturnItem ri " +
           "JOIN ri.drugReturn dr " +
           "WHERE b.batchNo = :batchNo " +
           "AND dr.status = 'APPROVED' " +
           "ORDER BY dr.createdAt DESC")
    List<DrugReturnItemBatch> findApprovedReturnsByBatchNo(@Param("batchNo") String batchNo);

    @Query("SELECT b FROM DrugReturnItemBatch b " +
           "JOIN b.drugReturnItem ri " +
           "JOIN ri.drugReturn dr " +
           "WHERE b.pending = true " +
           "AND dr.status = 'APPROVED' " +
           "ORDER BY dr.createdAt DESC")
    List<DrugReturnItemBatch> findPendingReturnBatches();

    @Query("SELECT b FROM DrugReturnItemBatch b " +
           "JOIN b.drugReturnItem ri " +
           "JOIN ri.drugReturn dr " +
           "WHERE b.drugCode = :drugCode " +
           "AND b.pending = true " +
           "AND dr.status = 'APPROVED' " +
           "ORDER BY dr.createdAt DESC")
    List<DrugReturnItemBatch> findPendingReturnsByDrugCode(@Param("drugCode") String drugCode);
}
