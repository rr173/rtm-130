package com.pharmacy.repository;

import com.pharmacy.entity.DispensingWindow;
import com.pharmacy.enums.DispensingWindowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispensingWindowRepository extends JpaRepository<DispensingWindow, Long> {

    Optional<DispensingWindow> findByWindowNo(String windowNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM DispensingWindow w WHERE w.windowNo = :windowNo")
    Optional<DispensingWindow> findByWindowNoWithLock(@Param("windowNo") String windowNo);

    List<DispensingWindow> findByStatus(DispensingWindowStatus status);

    List<DispensingWindow> findAllByOrderByWindowNoAsc();

    boolean existsByWindowNo(String windowNo);
}
