package com.pharmacy.service.reviewrule;

import com.pharmacy.dto.ReviewResultDTO;
import com.pharmacy.dto.reviewrule.GrayReleaseConfigDTO;
import com.pharmacy.dto.reviewrule.GrayReleaseCreateDTO;
import com.pharmacy.dto.reviewrule.GrayReleaseReportDTO;
import com.pharmacy.entity.reviewrule.GrayReleaseConfig;
import com.pharmacy.entity.reviewrule.GrayReviewComparison;
import com.pharmacy.entity.reviewrule.ReviewRuleConfig;
import com.pharmacy.enums.GrayReleaseStatus;
import com.pharmacy.enums.ReviewResultType;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.reviewrule.GrayReleaseConfigRepository;
import com.pharmacy.repository.reviewrule.GrayReviewComparisonRepository;
import com.pharmacy.repository.reviewrule.ReviewRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrayReleaseService {

    private final GrayReleaseConfigRepository grayReleaseRepository;
    private final GrayReviewComparisonRepository comparisonRepository;
    private final ReviewRuleConfigRepository configRepository;
    private final ReviewRuleConfigService configService;
    private final ReviewRuleConfigHolder configHolder;

    public Optional<GrayReleaseConfig> getActiveGrayRelease() {
        return grayReleaseRepository.findByStatus(GrayReleaseStatus.GRAYING);
    }

    public boolean isDepartmentInGray(String department) {
        if (department == null) {
            return false;
        }
        return getActiveGrayRelease()
                .map(g -> g.getDepartments().contains(department))
                .orElse(false);
    }

    public List<GrayReleaseConfigDTO> getAllGrayReleases() {
        return grayReleaseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    public GrayReleaseConfigDTO getGrayRelease(Long id) {
        GrayReleaseConfig config = grayReleaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("灰度发布不存在: " + id));
        return toDTO(config);
    }

    @Transactional
    public GrayReleaseConfigDTO createGrayRelease(GrayReleaseCreateDTO dto) {
        if (grayReleaseRepository.existsByStatus(GrayReleaseStatus.GRAYING)) {
            throw new BusinessException("已有进行中的灰度发布，请先完成或取消当前灰度");
        }

        if (grayReleaseRepository.existsByStatus(GrayReleaseStatus.PENDING)) {
            throw new BusinessException("已有待发布的灰度配置，请先处理");
        }

        ReviewRuleConfig newConfig = configRepository.findByVersion(dto.getNewConfigVersion())
                .orElseThrow(() -> new ResourceNotFoundException("新配置版本不存在: " + dto.getNewConfigVersion()));

        ReviewRuleConfig currentConfig = configHolder.getCurrentConfig();

        if (dto.getNewConfigVersion().equals(currentConfig.getVersion())) {
            throw new BusinessException("新配置版本不能与当前生效版本相同");
        }

        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setNewConfigVersion(dto.getNewConfigVersion());
        grayConfig.setBaseConfigVersion(currentConfig.getVersion());
        grayConfig.setStatus(GrayReleaseStatus.GRAYING);
        grayConfig.setDepartments(dto.getDepartments());
        grayConfig.setCreatedBy(dto.getCreatedBy());
        grayConfig.setRemark(dto.getRemark());
        grayConfig.setGrayStartTime(LocalDateTime.now());

        grayConfig = grayReleaseRepository.save(grayConfig);

        log.info("已创建灰度发布: 新版本={}, 基准版本={}, 科室={}, 创建人={}",
                grayConfig.getNewConfigVersion(),
                grayConfig.getBaseConfigVersion(),
                dto.getDepartments(),
                dto.getCreatedBy());

        return toDTO(grayConfig);
    }

    @Transactional
    public GrayReleaseConfigDTO cancelGrayRelease(Long id, String operator, String reason) {
        GrayReleaseConfig grayConfig = grayReleaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("灰度发布不存在: " + id));

        if (grayConfig.getStatus() != GrayReleaseStatus.GRAYING
                && grayConfig.getStatus() != GrayReleaseStatus.PENDING) {
            throw new BusinessException("当前灰度状态不允许取消");
        }

        grayConfig.setStatus(GrayReleaseStatus.CANCELLED);
        grayConfig.setCancelledBy(operator);
        grayConfig.setCancelTime(LocalDateTime.now());
        if (reason != null) {
            grayConfig.setRemark((grayConfig.getRemark() != null ? grayConfig.getRemark() + "; " : "")
                    + "取消原因: " + reason);
        }

        grayReleaseRepository.save(grayConfig);

        log.info("灰度发布已取消: id={}, 操作人={}, 原因={}", id, operator, reason);

        return toDTO(grayConfig);
    }

    @Transactional
    public GrayReleaseConfigDTO fullyRelease(Long id, String operator) {
        GrayReleaseConfig grayConfig = grayReleaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("灰度发布不存在: " + id));

        if (grayConfig.getStatus() != GrayReleaseStatus.GRAYING) {
            throw new BusinessException("只有灰度中的配置才能全量发布");
        }

        ReviewRuleConfig newConfig = configRepository.findByVersion(grayConfig.getNewConfigVersion())
                .orElseThrow(() -> new ResourceNotFoundException("配置版本不存在: " + grayConfig.getNewConfigVersion()));

        configHolder.updateConfig(newConfig);

        grayConfig.setStatus(GrayReleaseStatus.FULLY_RELEASED);
        grayConfig.setReleasedBy(operator);
        grayConfig.setFullReleaseTime(LocalDateTime.now());

        grayReleaseRepository.save(grayConfig);

        log.info("灰度配置已全量发布: id={}, 新版本={}, 操作人={}", id, grayConfig.getNewConfigVersion(), operator);

        return toDTO(grayConfig);
    }

    public GrayReleaseReportDTO generateReport(Long grayReleaseId) {
        GrayReleaseConfig grayConfig = grayReleaseRepository.findById(grayReleaseId)
                .orElseThrow(() -> new ResourceNotFoundException("灰度发布不存在: " + grayReleaseId));

        long total = comparisonRepository.countByGrayReleaseId(grayReleaseId);
        long consistent = comparisonRepository.countByGrayReleaseIdAndIsConsistent(grayReleaseId, true);
        long inconsistent = comparisonRepository.countByGrayReleaseIdAndIsConsistent(grayReleaseId, false);
        long moreBlocked = comparisonRepository.countMoreBlocked(grayReleaseId);
        long fewerBlocked = comparisonRepository.countFewerBlocked(grayReleaseId);

        double consistencyRate = total > 0 ? (double) consistent / total * 100 : 0.0;

        Map<ReviewResultType, Long> oldDist = new EnumMap<>(ReviewResultType.class);
        for (ReviewResultType type : ReviewResultType.values()) {
            oldDist.put(type, 0L);
        }
        for (Object[] row : comparisonRepository.countByOldRuleResultGrouped(grayReleaseId)) {
            oldDist.put((ReviewResultType) row[0], (Long) row[1]);
        }

        Map<ReviewResultType, Long> newDist = new EnumMap<>(ReviewResultType.class);
        for (ReviewResultType type : ReviewResultType.values()) {
            newDist.put(type, 0L);
        }
        for (Object[] row : comparisonRepository.countByNewRuleResultGrouped(grayReleaseId)) {
            newDist.put((ReviewResultType) row[0], (Long) row[1]);
        }

        return new GrayReleaseReportDTO(
                grayReleaseId,
                grayConfig.getNewConfigVersion(),
                grayConfig.getBaseConfigVersion(),
                total,
                consistent,
                inconsistent,
                consistencyRate,
                moreBlocked,
                fewerBlocked,
                oldDist,
                newDist,
                LocalDateTime.now()
        );
    }

    @Transactional
    public void recordComparison(Long grayReleaseId,
                                 String prescriptionNo,
                                 String department,
                                 String patientId,
                                 ReviewResultDTO oldResult,
                                 ReviewResultDTO newResult) {
        GrayReviewComparison comparison = new GrayReviewComparison();
        comparison.setGrayReleaseId(grayReleaseId);
        comparison.setPrescriptionNo(prescriptionNo);
        comparison.setDepartment(department);
        comparison.setPatientId(patientId);
        comparison.setOldRuleResult(oldResult.getOverallResult());
        comparison.setNewRuleResult(newResult.getOverallResult());
        comparison.setIsConsistent(oldResult.getOverallResult() == newResult.getOverallResult());
        comparison.setOldRuleDetails(buildResultSummary(oldResult));
        comparison.setNewRuleDetails(buildResultSummary(newResult));

        comparisonRepository.save(comparison);

        log.debug("灰度对比记录已保存: 处方={}, 旧规则={}, 新规则={}, 一致={}",
                prescriptionNo, oldResult.getOverallResult(),
                newResult.getOverallResult(), comparison.getIsConsistent());
    }

    private String buildResultSummary(ReviewResultDTO result) {
        StringBuilder sb = new StringBuilder();
        sb.append("总体结果: ").append(result.getOverallResult().getDescription());
        sb.append("; ");
        result.getRuleResults().forEach(r -> {
            sb.append(r.getRuleName()).append(": ")
                    .append(r.getResult().getDescription());
            if (r.getMessage() != null && !r.getMessage().isEmpty()) {
                sb.append("(").append(r.getMessage()).append(")");
            }
            sb.append("; ");
        });
        return sb.toString();
    }

    private GrayReleaseConfigDTO toDTO(GrayReleaseConfig entity) {
        return new GrayReleaseConfigDTO(
                entity.getId(),
                entity.getNewConfigVersion(),
                entity.getBaseConfigVersion(),
                entity.getStatus(),
                entity.getDepartments(),
                entity.getCreatedBy(),
                entity.getReleasedBy(),
                entity.getCancelledBy(),
                entity.getRemark(),
                entity.getGrayStartTime(),
                entity.getFullReleaseTime(),
                entity.getCancelTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
