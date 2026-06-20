package com.pharmacy.service.reviewrule;

import com.pharmacy.dto.reviewrule.ReviewRuleChangeLogDTO;
import com.pharmacy.dto.reviewrule.ReviewRuleConfigDTO;
import com.pharmacy.dto.reviewrule.ReviewRuleConfigUpdateDTO;
import com.pharmacy.entity.reviewrule.ReviewRuleChangeLog;
import com.pharmacy.entity.reviewrule.ReviewRuleConfig;
import com.pharmacy.enums.ReviewResultType;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.reviewrule.ReviewRuleChangeLogRepository;
import com.pharmacy.repository.reviewrule.ReviewRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewRuleConfigService {

    private final ReviewRuleConfigRepository configRepository;
    private final ReviewRuleChangeLogRepository changeLogRepository;
    private final ReviewRuleConfigHolder configHolder;

    private static final Map<String, String> FIELD_DISPLAY_NAMES = new HashMap<>();

    static {
        FIELD_DISPLAY_NAMES.put("maxSingleDoseMultiplier", "单品极量倍数");
        FIELD_DISPLAY_NAMES.put("duplicateMedicationWindowHours", "重复用药时间窗口(小时)");
        FIELD_DISPLAY_NAMES.put("severeContraindicationAction", "严重配伍禁忌处理策略");
        FIELD_DISPLAY_NAMES.put("moderateContraindicationAction", "中度配伍禁忌处理策略");
        FIELD_DISPLAY_NAMES.put("mildContraindicationAction", "轻度配伍禁忌处理策略");
    }

    @Transactional
    public void initializeDefaultConfig() {
        if (configRepository.count() == 0) {
            ReviewRuleConfig defaultConfig = ReviewRuleConfig.defaultConfig();
            configRepository.save(defaultConfig);
            configHolder.updateConfig(defaultConfig);
            log.info("已初始化默认审核规则配置，版本: {}", defaultConfig.getVersion());
        } else {
            ReviewRuleConfig latest = configRepository.findTopByOrderByVersionDesc().orElseThrow();
            configHolder.updateConfig(latest);
            log.info("已加载最新审核规则配置，版本: {}", latest.getVersion());
        }
    }

    public ReviewRuleConfigDTO getCurrentConfig() {
        return toDTO(configHolder.getCurrentConfig());
    }

    public List<ReviewRuleConfigDTO> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public ReviewRuleConfigDTO getConfigByVersion(Integer version) {
        ReviewRuleConfig config = configRepository.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("配置版本不存在: " + version));
        return toDTO(config);
    }

    @Transactional
    public ReviewRuleConfigDTO updateConfig(ReviewRuleConfigUpdateDTO dto) {
        ReviewRuleConfig currentConfig = configHolder.getCurrentConfig();

        List<ReviewRuleChangeLog> changeLogs = buildChangeLogs(currentConfig, dto);

        if (changeLogs.isEmpty()) {
            throw new BusinessException("配置未发生任何变更");
        }

        ReviewRuleConfig newConfig = buildNewConfig(currentConfig, dto);
        newConfig.setVersion(currentConfig.getVersion() + 1);
        newConfig.setUpdatedBy(dto.getOperator());
        newConfig.setCreatedBy(currentConfig.getCreatedBy());
        newConfig = configRepository.save(newConfig);

        LocalDateTime effectiveAt = LocalDateTime.now();
        for (ReviewRuleChangeLog log : changeLogs) {
            log.setConfigVersion(newConfig.getVersion());
            log.setOperator(dto.getOperator());
            log.setRemark(dto.getRemark());
            log.setEffectiveAt(effectiveAt);
            changeLogRepository.save(log);
        }

        configHolder.updateConfig(newConfig);

        log.info("审核规则配置已更新至版本: {}, 操作人: {}, 变更字段数: {}",
                newConfig.getVersion(), dto.getOperator(), changeLogs.size());

        return toDTO(newConfig);
    }

    @Transactional
    public ReviewRuleConfigDTO rollbackToVersion(Integer targetVersion, String operator, String remark) {
        ReviewRuleConfig targetConfig = configRepository.findByVersion(targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException("目标配置版本不存在: " + targetVersion));

        ReviewRuleConfig currentConfig = configHolder.getCurrentConfig();

        if (targetVersion.equals(currentConfig.getVersion())) {
            throw new BusinessException("目标版本与当前版本相同，无需回滚");
        }

        ReviewRuleConfigUpdateDTO rollbackDTO = new ReviewRuleConfigUpdateDTO();
        rollbackDTO.setMaxSingleDoseMultiplier(targetConfig.getMaxSingleDoseMultiplier());
        rollbackDTO.setDuplicateMedicationWindowHours(targetConfig.getDuplicateMedicationWindowHours());
        rollbackDTO.setSevereContraindicationAction(targetConfig.getSevereContraindicationAction());
        rollbackDTO.setModerateContraindicationAction(targetConfig.getModerateContraindicationAction());
        rollbackDTO.setMildContraindicationAction(targetConfig.getMildContraindicationAction());
        rollbackDTO.setDescription("回滚至版本 " + targetVersion + ": " + targetConfig.getDescription());
        rollbackDTO.setOperator(operator);
        rollbackDTO.setRemark(remark != null ? remark : "回滚到版本 " + targetVersion);

        log.info("回滚审核规则配置至版本: {}, 操作人: {}", targetVersion, operator);

        return updateConfig(rollbackDTO);
    }

    public List<ReviewRuleChangeLogDTO> getChangeLogs(Integer version) {
        List<ReviewRuleChangeLog> logs = version != null
                ? changeLogRepository.findByConfigVersionOrderByCreatedAtDesc(version)
                : changeLogRepository.findAllByOrderByCreatedAtDesc();
        return logs.stream().map(this::toChangeLogDTO).toList();
    }

    private List<ReviewRuleChangeLog> buildChangeLogs(ReviewRuleConfig oldConfig, ReviewRuleConfigUpdateDTO dto) {
        List<ReviewRuleChangeLog> logs = new ArrayList<>();

        checkAndAddLog(logs, oldConfig, dto,
                "maxSingleDoseMultiplier",
                ReviewRuleConfig::getMaxSingleDoseMultiplier,
                ReviewRuleConfigUpdateDTO::getMaxSingleDoseMultiplier,
                BigDecimal::toPlainString);

        checkAndAddLog(logs, oldConfig, dto,
                "duplicateMedicationWindowHours",
                c -> c.getDuplicateMedicationWindowHours(),
                ReviewRuleConfigUpdateDTO::getDuplicateMedicationWindowHours,
                Object::toString);

        checkAndAddLog(logs, oldConfig, dto,
                "severeContraindicationAction",
                ReviewRuleConfig::getSevereContraindicationAction,
                ReviewRuleConfigUpdateDTO::getSevereContraindicationAction,
                ReviewResultType::name);

        checkAndAddLog(logs, oldConfig, dto,
                "moderateContraindicationAction",
                ReviewRuleConfig::getModerateContraindicationAction,
                ReviewRuleConfigUpdateDTO::getModerateContraindicationAction,
                ReviewResultType::name);

        checkAndAddLog(logs, oldConfig, dto,
                "mildContraindicationAction",
                ReviewRuleConfig::getMildContraindicationAction,
                ReviewRuleConfigUpdateDTO::getMildContraindicationAction,
                ReviewResultType::name);

        return logs;
    }

    private <T> void checkAndAddLog(List<ReviewRuleChangeLog> logs,
                                    ReviewRuleConfig oldConfig,
                                    ReviewRuleConfigUpdateDTO dto,
                                    String fieldName,
                                    Function<ReviewRuleConfig, T> oldGetter,
                                    Function<ReviewRuleConfigUpdateDTO, T> newGetter,
                                    Function<T, String> toStringFunc) {
        T oldVal = oldGetter.apply(oldConfig);
        T newVal = newGetter.apply(dto);

        if ((oldVal == null && newVal != null)
                || (oldVal != null && !oldVal.equals(newVal))) {
            ReviewRuleChangeLog changeLog = new ReviewRuleChangeLog();
            changeLog.setFieldName(fieldName);
            changeLog.setFieldDisplayName(FIELD_DISPLAY_NAMES.getOrDefault(fieldName, fieldName));
            changeLog.setOldValue(oldVal != null ? toStringFunc.apply(oldVal) : null);
            changeLog.setNewValue(newVal != null ? toStringFunc.apply(newVal) : null);
            logs.add(changeLog);
        }
    }

    private ReviewRuleConfig buildNewConfig(ReviewRuleConfig oldConfig, ReviewRuleConfigUpdateDTO dto) {
        ReviewRuleConfig newConfig = new ReviewRuleConfig();
        newConfig.setMaxSingleDoseMultiplier(dto.getMaxSingleDoseMultiplier());
        newConfig.setDuplicateMedicationWindowHours(dto.getDuplicateMedicationWindowHours());
        newConfig.setSevereContraindicationAction(dto.getSevereContraindicationAction());
        newConfig.setModerateContraindicationAction(dto.getModerateContraindicationAction());
        newConfig.setMildContraindicationAction(dto.getMildContraindicationAction());
        newConfig.setDescription(dto.getDescription() != null ? dto.getDescription() : oldConfig.getDescription());
        return newConfig;
    }

    private ReviewRuleConfigDTO toDTO(ReviewRuleConfig entity) {
        return new ReviewRuleConfigDTO(
                entity.getId(),
                entity.getVersion(),
                entity.getMaxSingleDoseMultiplier(),
                entity.getDuplicateMedicationWindowHours(),
                entity.getSevereContraindicationAction(),
                entity.getModerateContraindicationAction(),
                entity.getMildContraindicationAction(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ReviewRuleChangeLogDTO toChangeLogDTO(ReviewRuleChangeLog entity) {
        return new ReviewRuleChangeLogDTO(
                entity.getId(),
                entity.getConfigVersion(),
                entity.getFieldName(),
                entity.getFieldDisplayName(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getOperator(),
                entity.getRemark(),
                entity.getCreatedAt(),
                entity.getEffectiveAt()
        );
    }
}
