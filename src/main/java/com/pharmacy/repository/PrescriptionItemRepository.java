package com.pharmacy.repository;

import com.pharmacy.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);

    @Query("SELECT pi FROM PrescriptionItem pi JOIN pi.prescription p " +
           "WHERE p.patientId = :patientId AND pi.drugCode = :drugCode " +
           "AND p.status NOT IN ('CANCELLED', 'REVIEW_BLOCKED') " +
           "AND p.createdAt >= :startTime ORDER BY p.createdAt DESC")
    List<PrescriptionItem> findRecentItemsForPatientAndDrug(
            @Param("patientId") String patientId,
            @Param("drugCode") String drugCode,
            @Param("startTime") java.time.LocalDateTime startTime);

    @Query("SELECT pi FROM PrescriptionItem pi JOIN pi.prescription p JOIN Drug d ON pi.drugCode = d.drugCode " +
           "WHERE p.patientId = :patientId " +
           "AND p.status NOT IN ('CANCELLED', 'REVIEW_BLOCKED') " +
           "AND p.createdAt >= :startTime " +
           "AND (d.ingredient LIKE CONCAT('%', :ingredient, '%') " +
           "     OR d.ingredient LIKE CONCAT(:ingredient, '%') " +
           "     OR d.ingredient LIKE CONCAT('%', :ingredient) " +
           "     OR d.ingredient = :ingredient) " +
           "ORDER BY p.createdAt DESC")
    List<PrescriptionItem> findRecentItemsForPatientAndIngredient(
            @Param("patientId") String patientId,
            @Param("ingredient") String ingredient,
            @Param("startTime") java.time.LocalDateTime startTime);
}
