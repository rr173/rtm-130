package com.pharmacy.repository;

import com.pharmacy.entity.DrugReturn;
import com.pharmacy.enums.DrugReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugReturnRepository extends JpaRepository<DrugReturn, Long> {

    Optional<DrugReturn> findByReturnNo(String returnNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DrugReturn d WHERE d.returnNo = :returnNo")
    Optional<DrugReturn> findByReturnNoWithLock(@Param("returnNo") String returnNo);

    List<DrugReturn> findByPrescriptionNoOrderByCreatedAtDesc(String prescriptionNo);

    List<DrugReturn> findByPatientIdOrderByCreatedAtDesc(String patientId);

    Page<DrugReturn> findByStatusOrderByCreatedAtDesc(DrugReturnStatus status, Pageable pageable);

    Page<DrugReturn> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT SUM(ri.returnQuantity) FROM DrugReturn dr " +
           "JOIN dr.items ri " +
           "WHERE dr.prescriptionNo = :prescriptionNo " +
           "AND ri.prescriptionItemId = :prescriptionItemId " +
           "AND dr.status IN ('APPROVED', 'PENDING_REVIEW')")
    Integer sumReturnedQuantityByPrescriptionItem(
            @Param("prescriptionNo") String prescriptionNo,
            @Param("prescriptionItemId") Long prescriptionItemId);
}
