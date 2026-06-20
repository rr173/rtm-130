package com.pharmacy.repository.reviewrule;

import com.pharmacy.entity.reviewrule.GrayReviewComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrayReviewComparisonRepository extends JpaRepository<GrayReviewComparison, Long> {

    List<GrayReviewComparison> findByGrayReleaseIdOrderByCreatedAtDesc(Long grayReleaseId);

    long countByGrayReleaseId(Long grayReleaseId);

    long countByGrayReleaseIdAndIsConsistent(Long grayReleaseId, Boolean isConsistent);

    @Query("SELECT g.oldRuleResult, COUNT(g) FROM GrayReviewComparison g WHERE g.grayReleaseId = :grayReleaseId GROUP BY g.oldRuleResult")
    List<Object[]> countByOldRuleResultGrouped(@Param("grayReleaseId") Long grayReleaseId);

    @Query("SELECT g.newRuleResult, COUNT(g) FROM GrayReviewComparison g WHERE g.grayReleaseId = :grayReleaseId GROUP BY g.newRuleResult")
    List<Object[]> countByNewRuleResultGrouped(@Param("grayReleaseId") Long grayReleaseId);

    @Query("SELECT COUNT(g) FROM GrayReviewComparison g WHERE g.grayReleaseId = :grayReleaseId AND g.oldRuleResult = 'BLOCKED' AND g.newRuleResult != 'BLOCKED'")
    long countFewerBlocked(@Param("grayReleaseId") Long grayReleaseId);

    @Query("SELECT COUNT(g) FROM GrayReviewComparison g WHERE g.grayReleaseId = :grayReleaseId AND g.oldRuleResult != 'BLOCKED' AND g.newRuleResult = 'BLOCKED'")
    long countMoreBlocked(@Param("grayReleaseId") Long grayReleaseId);
}
