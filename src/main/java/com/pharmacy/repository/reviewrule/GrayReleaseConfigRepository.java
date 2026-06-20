package com.pharmacy.repository.reviewrule;

import com.pharmacy.entity.reviewrule.GrayReleaseConfig;
import com.pharmacy.enums.GrayReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrayReleaseConfigRepository extends JpaRepository<GrayReleaseConfig, Long> {

    Optional<GrayReleaseConfig> findByNewConfigVersion(Integer newConfigVersion);

    Optional<GrayReleaseConfig> findByStatus(GrayReleaseStatus status);

    List<GrayReleaseConfig> findAllByOrderByCreatedAtDesc();

    boolean existsByStatus(GrayReleaseStatus status);
}
