package com.pharmacy.repository;

import com.pharmacy.entity.ConsultationOpinion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationOpinionRepository extends JpaRepository<ConsultationOpinion, Long> {

    List<ConsultationOpinion> findByConsultationIdOrderByCreatedAtAsc(Long consultationId);

    Optional<ConsultationOpinion> findByConsultationIdAndPharmacistId(Long consultationId, String pharmacistId);

    List<ConsultationOpinion> findByPharmacistIdOrderByCreatedAtDesc(String pharmacistId);

    @Query("SELECT o FROM ConsultationOpinion o WHERE o.pharmacistId = :pharmacistId " +
           "AND o.submittedAt BETWEEN :startTime AND :endTime ORDER BY o.submittedAt DESC")
    List<ConsultationOpinion> findByPharmacistIdAndSubmittedAtBetween(
            @Param("pharmacistId") String pharmacistId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT o FROM ConsultationOpinion o WHERE o.consultation.id = :consultationId " +
           "AND o.submittedAt IS NULL AND o.deadline < :now AND o.isAbstained = false")
    List<ConsultationOpinion> findTimeoutOpinions(
            @Param("consultationId") Long consultationId,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(o) FROM ConsultationOpinion o WHERE o.consultation.id = :consultationId " +
           "AND (o.submittedAt IS NOT NULL OR o.isAbstained = true)")
    long countCompletedOpinionsByConsultationId(@Param("consultationId") Long consultationId);

    @Query("SELECT COUNT(o) FROM ConsultationOpinion o WHERE o.consultation.id = :consultationId")
    long countTotalOpinionsByConsultationId(@Param("consultationId") Long consultationId);

    @Query("SELECT o FROM ConsultationOpinion o WHERE o.deadline < :now AND o.submittedAt IS NULL AND o.isAbstained = false")
    List<ConsultationOpinion> findAllTimeoutOpinions(@Param("now") LocalDateTime now);
}
