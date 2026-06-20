package com.pharmacy.repository.reviewrule;

import com.pharmacy.entity.reviewrule.ReviewRuleChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRuleChangeLogRepository extends JpaRepository<ReviewRuleChangeLog, Long> {

    List<ReviewRuleChangeLog> findByConfigVersionOrderByCreatedAtDesc(Integer configVersion);

    List<ReviewRuleChangeLog> findAllByOrderByCreatedAtDesc();
}
