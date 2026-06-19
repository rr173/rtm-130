package com.pharmacy.repository;

import com.pharmacy.entity.DispenseQueueItem;
import com.pharmacy.enums.DispenseChannel;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM DispenseQueueItem q WHERE q.status = :status ORDER BY q.sortPriority ASC, q.enqueueTime ASC")
    List<DispenseQueueItem> findNextWaitingWithLock(@Param("status") QueueItemStatus status, Pageable pageable);

    default Optional<DispenseQueueItem> findNextWaiting() {
        List<DispenseQueueItem> results = findNextWaitingWithLock(QueueItemStatus.WAITING, Pageable.ofSize(1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM DispenseQueueItem q WHERE q.status = :status AND q.channel IN :channels " +
           "ORDER BY q.sortPriority ASC, q.enqueueTime ASC")
    List<DispenseQueueItem> findNextWaitingByChannelsWithLock(
            @Param("status") QueueItemStatus status,
            @Param("channels") List<DispenseChannel> channels,
            Pageable pageable);

    default Optional<DispenseQueueItem> findNextWaitingByChannels(List<DispenseChannel> channels) {
        List<DispenseQueueItem> results = findNextWaitingByChannelsWithLock(
                QueueItemStatus.WAITING, channels, Pageable.ofSize(1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Query("SELECT q FROM DispenseQueueItem q WHERE q.status = :status ORDER BY q.sortPriority ASC, q.enqueueTime ASC")
    List<DispenseQueueItem> findAllWaitingOrdered(@Param("status") QueueItemStatus status);

    default List<DispenseQueueItem> findAllWaitingOrdered() {
        return findAllWaitingOrdered(QueueItemStatus.WAITING);
    }

    @Query("SELECT q FROM DispenseQueueItem q WHERE q.status = :status AND q.channel = :channel " +
           "ORDER BY q.sortPriority ASC, q.enqueueTime ASC")
    List<DispenseQueueItem> findAllWaitingOrderedByChannel(
            @Param("status") QueueItemStatus status,
            @Param("channel") DispenseChannel channel);

    default List<DispenseQueueItem> findAllWaitingOrderedByChannel(DispenseChannel channel) {
        return findAllWaitingOrderedByChannel(QueueItemStatus.WAITING, channel);
    }

    long countByStatus(QueueItemStatus status);

    @Query("SELECT COUNT(q) FROM DispenseQueueItem q WHERE q.status = :status AND q.channel = :channel")
    long countWaitingByChannel(@Param("status") QueueItemStatus status, @Param("channel") DispenseChannel channel);

    default long countWaitingByChannel(DispenseChannel channel) {
        return countWaitingByChannel(QueueItemStatus.WAITING, channel);
    }

    @Query("SELECT COUNT(q) FROM DispenseQueueItem q WHERE q.status = :status AND q.sortPriority < 200")
    long countEmergencyWaiting(@Param("status") QueueItemStatus status);

    default long countEmergencyWaiting() {
        return countEmergencyWaiting(QueueItemStatus.WAITING);
    }

    @Query("SELECT COUNT(q) FROM DispenseQueueItem q WHERE q.status = :status AND q.sortPriority >= 200")
    long countNormalWaiting(@Param("status") QueueItemStatus status);

    default long countNormalWaiting() {
        return countNormalWaiting(QueueItemStatus.WAITING);
    }

    @Query("SELECT MIN(q.sortPriority) FROM DispenseQueueItem q WHERE q.status = :status")
    Integer findMinSortPriority(@Param("status") QueueItemStatus status);

    @Query("SELECT MIN(q.enqueueTime) FROM DispenseQueueItem q WHERE q.status = :status AND q.sortPriority = :priority")
    java.time.LocalDateTime findMinEnqueueTime(@Param("status") QueueItemStatus status, @Param("priority") Integer priority);

    @Query("SELECT q.channel, COUNT(q) FROM DispenseQueueItem q WHERE q.status = :status GROUP BY q.channel")
    List<Object[]> countWaitingGroupByChannel(@Param("status") QueueItemStatus status);

    List<DispenseQueueItem> findByStatusAndWindowNo(QueueItemStatus status, String windowNo);

    List<DispenseQueueItem> findByPrescriptionNo(String prescriptionNo);
}
