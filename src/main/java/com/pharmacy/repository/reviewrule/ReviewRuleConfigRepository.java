package com.pharmacy.repository.reviewrule;

import com.pharmacy.entity.reviewrule.ReviewRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRuleConfigRepository extends JpaRepository<ReviewRuleConfig, Long> {

    Optional<ReviewRuleConfig> findByVersion(Integer version);

    Optional<ReviewRuleConfig> findTopByOrderByVersionDesc();
}
