package com.pharmacy.repository;

import com.pharmacy.entity.DispenseQueueItem;
import com.pharmacy.enums.QueueItemStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispenseQueueItemRepository extends JpaRepository<DispenseQueueItem, Long> {

    Optional<DispenseQueueItem> findByPrescriptionNoAndStatus(String prescriptionNo, QueueItemStatus status);

    List<DispenseQueueItem> findByStatusOrderByPrescriptionTypeAscEnqueueTimeAsc(QueueItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM DispenseQueueItem q WHERE q.status = :status ORDER BY q.prescriptionType ASC, q.enqueueTime ASC")
    List<DispenseQueueItem> findNextWaitingWithLock(@Param("status") QueueItemStatus status, Pageable pageable);

    default Optional<DispenseQueueItem> findNextWaiting() {
        List<DispenseQueueItem> results = findNextWaitingWithLock(QueueItemStatus.WAITING, Pageable.ofSize(1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Query("SELECT q FROM DispenseQueueItem q WHERE q.status = :status ORDER BY q.prescriptionType ASC, q.enqueueTime ASC")
    List<DispenseQueueItem> findAllWaitingOrdered(@Param("status") QueueItemStatus status);

    default List<DispenseQueueItem> findAllWaitingOrdered() {
        return findAllWaitingOrdered(QueueItemStatus.WAITING);
    }

    long countByStatus(QueueItemStatus status);

    @Query("SELECT COUNT(q) FROM DispenseQueueItem q WHERE q.status = :status AND q.prescriptionType = 'EMERGENCY'")
    long countEmergencyWaiting(@Param("status") QueueItemStatus status);

    default long countEmergencyWaiting() {
        return countEmergencyWaiting(QueueItemStatus.WAITING);
    }

    @Query("SELECT COUNT(q) FROM DispenseQueueItem q WHERE q.status = :status AND q.prescriptionType <> 'EMERGENCY'")
    long countNormalWaiting(@Param("status") QueueItemStatus status);

    default long countNormalWaiting() {
        return countNormalWaiting(QueueItemStatus.WAITING);
    }

    List<DispenseQueueItem> findByStatusAndWindowNo(QueueItemStatus status, String windowNo);

    List<DispenseQueueItem> findByPrescriptionNo(String prescriptionNo);
}
