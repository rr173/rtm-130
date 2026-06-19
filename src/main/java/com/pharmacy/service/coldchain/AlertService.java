package com.pharmacy.service.coldchain;

import com.pharmacy.config.ColdChainConfig;
import com.pharmacy.dto.coldchain.AlertEventDTO;
import com.pharmacy.entity.AlertEvent;
import com.pharmacy.entity.DrugBatch;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.entity.TempHumidityReading;
import com.pharmacy.enums.AlertLevel;
import com.pharmacy.enums.AlertStatus;
import com.pharmacy.enums.BatchStatus;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.AlertEventRepository;
import com.pharmacy.repository.DrugBatchRepository;
import com.pharmacy.repository.MonitoringPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertEventRepository alertEventRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final ColdChainConfig coldChainConfig;

    @Transactional
    public AlertEvent createAlert(MonitoringPoint point, TempHumidityReading reading,
                                   AlertLevel level, BigDecimal tempDeviation, BigDecimal humidityDeviation) {
        AlertEvent alert = new AlertEvent();
        alert.setPointCode(point.getPointCode());
        alert.setAlertLevel(level);
        alert.setAlertStatus(AlertStatus.ACTIVE);
        alert.setTriggerTemperature(reading.getTemperature());
        alert.setTriggerHumidity(reading.getHumidity());
        alert.setTempLowerLimit(point.getTempMin());
        alert.setTempUpperLimit(point.getTempMax());
        alert.setHumidityLowerLimit(point.getHumidityMin());
        alert.setHumidityUpperLimit(point.getHumidityMax());
        alert.setFirstTriggerTime(reading.getCollectTime());
        alert.setLastTriggerTime(reading.getCollectTime());

        StringBuilder desc = new StringBuilder();
        desc.append(level.getDescription()).append(" - ");
        if (reading.getTempOutOfRange()) {
            desc.append("温度").append(reading.getTemperature()).append("℃");
            if (reading.getTemperature().compareTo(point.getTempMax()) > 0) {
                desc.append("超出上限").append(tempDeviation).append("℃");
            } else {
                desc.append("低于下限").append(tempDeviation).append("℃");
            }
        }
        if (reading.getTempOutOfRange() && reading.getHumidityOutOfRange()) {
            desc.append(", ");
        }
        if (reading.getHumidityOutOfRange()) {
            desc.append("湿度").append(reading.getHumidity()).append("%RH");
            if (reading.getHumidity().compareTo(point.getHumidityMax()) > 0) {
                desc.append("超出上限").append(humidityDeviation).append("%RH");
            } else {
                desc.append("低于下限").append(humidityDeviation).append("%RH");
            }
        }
        alert.setDescription(desc.toString());
        alert = alertEventRepository.save(alert);

        log.warn("创建告警: pointCode={}, level={}, id={}, desc={}",
                point.getPointCode(), level, alert.getId(), desc);

        if (level == AlertLevel.RED) {
            triggerColdChainInterruption(point, alert);
        }

        return alert;
    }

    @Transactional
    public AlertEvent upgradeAlert(AlertEvent originalAlert, AlertLevel newLevel,
                                    TempHumidityReading reading, String reason) {
        originalAlert.setAlertStatus(AlertStatus.UPGRADED);
        alertEventRepository.save(originalAlert);

        AlertEvent upgradedAlert = new AlertEvent();
        upgradedAlert.setPointCode(originalAlert.getPointCode());
        upgradedAlert.setAlertLevel(newLevel);
        upgradedAlert.setAlertStatus(AlertStatus.ACTIVE);
        if (reading != null) {
            upgradedAlert.setTriggerTemperature(reading.getTemperature());
            upgradedAlert.setTriggerHumidity(reading.getHumidity());
        } else {
            upgradedAlert.setTriggerTemperature(originalAlert.getTriggerTemperature());
            upgradedAlert.setTriggerHumidity(originalAlert.getTriggerHumidity());
        }
        upgradedAlert.setTempLowerLimit(originalAlert.getTempLowerLimit());
        upgradedAlert.setTempUpperLimit(originalAlert.getTempUpperLimit());
        upgradedAlert.setHumidityLowerLimit(originalAlert.getHumidityLowerLimit());
        upgradedAlert.setHumidityUpperLimit(originalAlert.getHumidityUpperLimit());
        upgradedAlert.setFirstTriggerTime(originalAlert.getFirstTriggerTime());
        upgradedAlert.setLastTriggerTime(reading != null ? reading.getCollectTime() : LocalDateTime.now());
        upgradedAlert.setParentAlertId(originalAlert.getId());
        upgradedAlert.setDescription("告警升级: " + originalAlert.getAlertLevel().getDescription()
                + " -> " + newLevel.getDescription() + ", 原因: " + reason
                + (reading != null ? ", 当前读数: 温度" + reading.getTemperature() + "℃, 湿度" + reading.getHumidity() + "%RH" : ""));
        upgradedAlert = alertEventRepository.save(upgradedAlert);

        log.warn("告警升级: originalId={}, newId={}, {} -> {}, reason={}",
                originalAlert.getId(), upgradedAlert.getId(),
                originalAlert.getAlertLevel(), newLevel, reason);

        if (newLevel == AlertLevel.RED) {
            MonitoringPoint point = monitoringPointRepository.findByPointCode(originalAlert.getPointCode())
                    .orElse(null);
            if (point != null) {
                triggerColdChainInterruption(point, upgradedAlert);
            }
        }

        return upgradedAlert;
    }

    @Transactional
    public void resolveAlert(AlertEvent alert, LocalDateTime resolvedTime,
                              String resolvedBy, String remark) {
        AlertEvent rootAlert = findRootAlert(alert);
        resolveAlertChain(rootAlert, resolvedTime, resolvedBy, remark);

        log.info("告警恢复: rootAlertId={}, resolvedBy={}, duration={}分钟",
                rootAlert.getId(), resolvedBy,
                Duration.between(rootAlert.getFirstTriggerTime(), resolvedTime).toMinutes());
    }

    private AlertEvent findRootAlert(AlertEvent alert) {
        AlertEvent current = alert;
        while (current.getParentAlertId() != null) {
            current = alertEventRepository.findById(current.getParentAlertId())
                    .orElse(null);
            if (current == null) break;
        }
        return current != null ? current : alert;
    }

    private void resolveAlertChain(AlertEvent root, LocalDateTime resolvedTime,
                                    String resolvedBy, String remark) {
        if (root.getAlertStatus() != AlertStatus.RESOLVED) {
            root.setAlertStatus(AlertStatus.RESOLVED);
            root.setResolvedTime(resolvedTime);
            root.setResolvedBy(resolvedBy);
            root.setResolvedRemark(remark);
            alertEventRepository.save(root);
        }

        List<AlertEvent> children = alertEventRepository.findByParentAlertId(root.getId());
        for (AlertEvent child : children) {
            resolveAlertChain(child, resolvedTime, resolvedBy, remark);
        }
    }

    @Transactional
    public void triggerColdChainInterruption(MonitoringPoint point, AlertEvent redAlert) {
        List<String> boundBatchNos = point.getBoundBatchNos();
        if (boundBatchNos == null || boundBatchNos.isEmpty()) {
            log.warn("红色告警但监控点{}未绑定药品批次", point.getPointCode());
            return;
        }

        List<DrugBatch> batchesToUpdate = new ArrayList<>();
        for (String batchNo : boundBatchNos) {
            List<DrugBatch> batches = drugBatchRepository.findAllByBatchNo(batchNo);
            for (DrugBatch batch : batches) {
                if (batch.getStatus() != BatchStatus.COLD_CHAIN_PENDING
                        && batch.getStatus() != BatchStatus.COLD_CHAIN_REJECTED
                        && batch.getStatus() != BatchStatus.LOCKED) {
                    batch.setStatus(BatchStatus.COLD_CHAIN_PENDING);
                    batchesToUpdate.add(batch);
                    log.info("标记批次冷链中断待检: batchNo={}, drugName={}, alertId={}",
                            batchNo, batch.getDrugName(), redAlert.getId());
                }
            }
        }
        drugBatchRepository.saveAll(batchesToUpdate);
        log.warn("冷链中断触发: pointCode={}, alertId={}, 影响批次数量={}",
                point.getPointCode(), redAlert.getId(), batchesToUpdate.size());
    }

    public List<AlertEventDTO> getActiveAlertsByLevel(AlertLevel level) {
        List<AlertStatus> activeStatuses = List.of(AlertStatus.ACTIVE);
        List<AlertEvent> alerts;
        if (level != null) {
            alerts = alertEventRepository.findByLevelAndStatusIn(level, activeStatuses);
        } else {
            alerts = alertEventRepository.findByStatusIn(activeStatuses);
        }
        return alerts.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<AlertEventDTO> getAlertsByTimeRange(String pointCode,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime) {
        List<AlertStatus> allStatuses = Arrays.asList(AlertStatus.values());
        List<AlertEvent> alerts = alertEventRepository.findByPointCodeAndStatusInAndTimeRange(
                pointCode, allStatuses, startTime, endTime);
        return alerts.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public AlertEventDTO getAlertById(Long alertId) {
        AlertEvent alert = alertEventRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("告警不存在: " + alertId));
        return convertToDTO(alert);
    }

    public AlertEvent getAlertEntity(Long alertId) {
        return alertEventRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("告警不存在: " + alertId));
    }

    @Transactional
    public void checkAndUpgradeAlerts() {
        ColdChainConfig.UpgradeThresholds thresholds = coldChainConfig.getUpgradeThresholds();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime yellowThreshold = now.minusMinutes(thresholds.getYellowToOrangeMinutes());
        List<AlertEvent> yellowAlerts = alertEventRepository.findAlertsEligibleForUpgrade(
                AlertStatus.ACTIVE, AlertLevel.YELLOW, yellowThreshold);
        for (AlertEvent alert : yellowAlerts) {
            log.info("黄色预警超时自动升级: alertId={}, 持续时间={}分钟",
                    alert.getId(),
                    Duration.between(alert.getFirstTriggerTime(), now).toMinutes());
            upgradeAlert(alert, AlertLevel.ORANGE, null,
                    "黄色预警持续超过" + thresholds.getYellowToOrangeMinutes() + "分钟未恢复，自动升级为橙色告警");
        }

        LocalDateTime orangeThreshold = now.minusMinutes(thresholds.getOrangeToRedMinutes());
        List<AlertEvent> orangeAlerts = alertEventRepository.findAlertsEligibleForUpgrade(
                AlertStatus.ACTIVE, AlertLevel.ORANGE, orangeThreshold);
        for (AlertEvent alert : orangeAlerts) {
            log.info("橙色告警超时自动升级: alertId={}, 持续时间={}分钟",
                    alert.getId(),
                    Duration.between(alert.getFirstTriggerTime(), now).toMinutes());
            upgradeAlert(alert, AlertLevel.RED, null,
                    "橙色告警持续超过" + thresholds.getOrangeToRedMinutes() + "分钟未恢复，自动升级为红色告警");
        }
    }

    public List<AlertEvent> getAlertChain(Long alertId) {
        AlertEvent root = findRootAlert(alertEventRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("告警不存在: " + alertId)));
        List<AlertEvent> chain = new ArrayList<>();
        chain.add(root);
        collectChildren(root.getId(), chain);
        return chain;
    }

    private void collectChildren(Long parentId, List<AlertEvent> chain) {
        List<AlertEvent> children = alertEventRepository.findByParentAlertId(parentId);
        for (AlertEvent child : children) {
            chain.add(child);
            collectChildren(child.getId(), chain);
        }
    }

    private AlertEventDTO convertToDTO(AlertEvent alert) {
        AlertEventDTO dto = new AlertEventDTO();
        dto.setId(alert.getId());
        dto.setPointCode(alert.getPointCode());
        monitoringPointRepository.findByPointCode(alert.getPointCode())
                .ifPresent(p -> dto.setPointName(p.getPointName()));
        dto.setAlertLevel(alert.getAlertLevel());
        dto.setAlertLevelDescription(alert.getAlertLevel().getDescription());
        dto.setAlertStatus(alert.getAlertStatus());
        dto.setAlertStatusDescription(alert.getAlertStatus().getDescription());
        dto.setTriggerTemperature(alert.getTriggerTemperature());
        dto.setTriggerHumidity(alert.getTriggerHumidity());
        dto.setTempLowerLimit(alert.getTempLowerLimit());
        dto.setTempUpperLimit(alert.getTempUpperLimit());
        dto.setHumidityLowerLimit(alert.getHumidityLowerLimit());
        dto.setHumidityUpperLimit(alert.getHumidityUpperLimit());
        dto.setFirstTriggerTime(alert.getFirstTriggerTime());
        dto.setLastTriggerTime(alert.getLastTriggerTime());
        dto.setResolvedTime(alert.getResolvedTime());
        dto.setParentAlertId(alert.getParentAlertId());
        dto.setDescription(alert.getDescription());
        LocalDateTime endTime = alert.getResolvedTime() != null ? alert.getResolvedTime() : LocalDateTime.now();
        dto.setDurationMinutes(Duration.between(alert.getFirstTriggerTime(), endTime).toMinutes());
        return dto;
    }
}
