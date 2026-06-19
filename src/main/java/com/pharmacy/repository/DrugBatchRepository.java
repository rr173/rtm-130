package com.pharmacy.repository;

import com.pharmacy.entity.DrugBatch;
import com.pharmacy.enums.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrugBatchRepository extends JpaRepository<DrugBatch, Long> {

    Optional<DrugBatch> findByDrugCodeAndBatchNo(String drugCode, String batchNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DrugBatch b WHERE b.drugCode = :drugCode AND b.batchNo = :batchNo")
    Optional<DrugBatch> findByDrugCodeAndBatchNoWithLock(@Param("drugCode") String drugCode, @Param("batchNo") String batchNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DrugBatch b WHERE b.id = :id")
    Optional<DrugBatch> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT b FROM DrugBatch b WHERE b.drugCode = :drugCode " +
           "AND b.status NOT IN :excludedStatuses " +
           "AND (b.availableQuantity > 0 OR b.splitQuantity > 0) " +
           "ORDER BY b.expiryDate ASC, b.createdAt ASC")
    List<DrugBatch> findAvailableBatchesByDrugCode(@Param("drugCode") String drugCode,
                                                    @Param("excludedStatuses") List<BatchStatus> excludedStatuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DrugBatch b WHERE b.drugCode = :drugCode " +
           "AND b.status NOT IN :excludedStatuses " +
           "AND (b.availableQuantity > 0 OR b.splitQuantity > 0) " +
           "ORDER BY b.expiryDate ASC, b.createdAt ASC")
    List<DrugBatch> findAvailableBatchesByDrugCodeWithLock(@Param("drugCode") String drugCode,
                                                            @Param("excludedStatuses") List<BatchStatus> excludedStatuses);

    List<DrugBatch> findByDrugCodeOrderByExpiryDateAsc(String drugCode);

    List<DrugBatch> findByStatusIn(List<BatchStatus> statuses);

    @Query("SELECT b FROM DrugBatch b WHERE b.expiryDate <= :expiryDate " +
           "AND b.status NOT IN :excludedStatuses")
    List<DrugBatch> findBatchesExpiringBefore(@Param("expiryDate") LocalDate expiryDate,
                                               @Param("excludedStatuses") List<BatchStatus> excludedStatuses);

    @Query("SELECT b FROM DrugBatch b WHERE b.status NOT IN :excludedStatuses " +
           "AND b.expiryDate BETWEEN :startDate AND :endDate")
    List<DrugBatch> findBatchesExpiringBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("excludedStatuses") List<BatchStatus> excludedStatuses);

    @Query("SELECT b FROM DrugBatch b WHERE b.drugCode = :drugCode " +
           "AND b.status NOT IN :excludedStatuses " +
           "AND b.splitLocked = false " +
           "AND b.availableQuantity >= 1 " +
           "ORDER BY b.expiryDate ASC, b.createdAt ASC")
    List<DrugBatch> findSplittableBatchesByDrugCode(@Param("drugCode") String drugCode,
                                                     @Param("excludedStatuses") List<BatchStatus> excludedStatuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DrugBatch b WHERE b.id = :batchId AND b.splitLocked = false")
    Optional<DrugBatch> findSplittableBatchByIdWithLock(@Param("batchId") Long batchId);

    List<DrugBatch> findByBatchNo(String batchNo);

    @Query("SELECT b FROM DrugBatch b WHERE b.batchNo = :batchNo")
    List<DrugBatch> findAllByBatchNo(@Param("batchNo") String batchNo);
}
