package com.pharmacy.service.coldchain;

import com.pharmacy.config.ColdChainConfig;
import com.pharmacy.dto.coldchain.BatchEnvironmentHistoryDTO;
import com.pharmacy.dto.coldchain.DisposalRequestDTO;
import com.pharmacy.dto.coldchain.PendingDisposalBatchDTO;
import com.pharmacy.entity.AlertEvent;
import com.pharmacy.entity.ColdChainDisposalRecord;
import com.pharmacy.entity.Drug;
import com.pharmacy.entity.DrugBatch;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.enums.AlertLevel;
import com.pharmacy.enums.AlertStatus;
import com.pharmacy.enums.BatchStatus;
import com.pharmacy.enums.DisposalDecision;
import com.pharmacy.enums.DrugCategory;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.AlertEventRepository;
import com.pharmacy.repository.ColdChainDisposalRecordRepository;
import com.pharmacy.repository.DrugBatchRepository;
import com.pharmacy.repository.DrugRepository;
import com.pharmacy.repository.MonitoringPointRepository;
import com.pharmacy.repository.TempHumidityReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColdChainDisposalService {

    private final DrugBatchRepository drugBatchRepository;
    private final DrugRepository drugRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final AlertEventRepository alertEventRepository;
    private final TempHumidityReadingRepository readingRepository;
    private final ColdChainDisposalRecordRepository disposalRecordRepository;
    private final ColdChainConfig coldChainConfig;

    public List<PendingDisposalBatchDTO> getPendingDisposalBatches() {
        List<DrugBatch> pendingBatches = drugBatchRepository.findByStatusIn(
                List.of(BatchStatus.COLD_CHAIN_PENDING));

        return pendingBatches.stream()
                .map(this::convertToPendingDTO)
                .collect(Collectors.toList());
    }

    private PendingDisposalBatchDTO convertToPendingDTO(DrugBatch batch) {
        PendingDisposalBatchDTO dto = new PendingDisposalBatchDTO();
        dto.setBatchId(batch.getId());
        dto.setDrugCode(batch.getDrugCode());
        dto.setDrugName(batch.getDrugName());
        dto.setBatchNo(batch.getBatchNo());
        dto.setAvailableQuantity(batch.getAvailableQuantity() + batch.getSplitQuantity());

        List<MonitoringPoint> points = monitoringPointRepository.findByBoundBatchNo(batch.getBatchNo());
        if (!points.isEmpty()) {
            MonitoringPoint point = points.get(0);
            dto.setPointCode(point.getPointCode());
            dto.setPointName(point.getPointName());

            List<AlertStatus> activeStatuses = List.of(AlertStatus.ACTIVE, AlertStatus.RESOLVED);
            List<AlertEvent> redAlerts = alertEventRepository.findByLevelAndStatusIn(AlertLevel.RED, activeStatuses)
                    .stream()
                    .filter(a -> a.getPointCode().equals(point.getPointCode()))
                    .filter(a -> {
                        if (a.getResolvedTime() != null) {
                            return a.getResolvedTime().isAfter(a.getFirstTriggerTime());
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            if (!redAlerts.isEmpty()) {
                AlertEvent alert = redAlerts.get(0);
                dto.setAlertId(alert.getId());
                dto.setInterruptionStartTime(alert.getFirstTriggerTime());

                LocalDateTime endTime = alert.getResolvedTime() != null ? alert.getResolvedTime() : LocalDateTime.now();
                long minutes = Duration.between(alert.getFirstTriggerTime(), endTime).toMinutes();
                dto.setInterruptionDurationMinutes(BigDecimal.valueOf(minutes));

                Drug drug = drugRepository.findByDrugCode(batch.getDrugCode()).orElse(null);
                DrugCategory category = drug != null ? drug.getCategory() : DrugCategory.NORMAL;
                dto.setMaxToleranceMinutes(coldChainConfig.getMaxToleranceMinutes(category));
                dto.setDrugExtremeTemperature(coldChainConfig.getExtremeTemperature(category));

                BigDecimal maxTemp = readingRepository.findMaxTemperatureByPointCodeAndTimeRange(
                        point.getPointCode(), alert.getFirstTriggerTime(), endTime);
                BigDecimal minTemp = readingRepository.findMinTemperatureByPointCodeAndTimeRange(
                        point.getPointCode(), alert.getFirstTriggerTime(), endTime);
                dto.setMaxRecordedTemperature(maxTemp);
                dto.setMinRecordedTemperature(minTemp);

                BigDecimal extremeTemp = dto.getDrugExtremeTemperature();
                boolean withinDuration = dto.getInterruptionDurationMinutes()
                        .compareTo(dto.getMaxToleranceMinutes()) <= 0;
                boolean withinTemp = true;
                if (maxTemp != null && extremeTemp != null) {
                    withinTemp = maxTemp.compareTo(extremeTemp) <= 0;
                }
                if (minTemp != null) {
                    BigDecimal minExtreme = BigDecimal.ZERO;
                    withinTemp = withinTemp && (minTemp.compareTo(minExtreme) >= 0);
                }
                dto.setWithinTolerance(withinDuration && withinTemp);
            }
        }

        return dto;
    }

    @Transactional
    public ColdChainDisposalRecord disposeBatch(DisposalRequestDTO request) {
        DrugBatch batch = drugBatchRepository.findByIdWithLock(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("药品批次不存在: " + request.getBatchId()));

        if (batch.getStatus() != BatchStatus.COLD_CHAIN_PENDING) {
            throw new BusinessException("该批次状态不允许处置，当前状态: " + batch.getStatus().getDescription());
        }

        if (disposalRecordRepository.existsByBatchNoAndAlertId(batch.getBatchNo(), request.getAlertId())) {
            throw new BusinessException("该批次已针对此告警进行过处置");
        }

        AlertEvent alert = alertEventRepository.findById(request.getAlertId())
                .orElseThrow(() -> new ResourceNotFoundException("告警不存在: " + request.getAlertId()));

        if (alert.getAlertLevel() != AlertLevel.RED) {
            throw new BusinessException("仅红色严重告警批次需要冷链处置");
        }

        Drug drug = drugRepository.findByDrugCode(batch.getDrugCode()).orElse(null);
        DrugCategory category = drug != null ? drug.getCategory() : DrugCategory.NORMAL;

        LocalDateTime interruptionStartTime = alert.getFirstTriggerTime();
        LocalDateTime interruptionEndTime = alert.getResolvedTime() != null
                ? alert.getResolvedTime() : LocalDateTime.now();
        BigDecimal interruptionDuration = BigDecimal.valueOf(
                Duration.between(interruptionStartTime, interruptionEndTime).toMinutes());

        BigDecimal maxTolerance = coldChainConfig.getMaxToleranceMinutes(category);
        BigDecimal extremeTemp = coldChainConfig.getExtremeTemperature(category);

        BigDecimal maxRecordedTemp = readingRepository.findMaxTemperatureByPointCodeAndTimeRange(
                alert.getPointCode(), interruptionStartTime, interruptionEndTime);
        BigDecimal minRecordedTemp = readingRepository.findMinTemperatureByPointCodeAndTimeRange(
                alert.getPointCode(), interruptionStartTime, interruptionEndTime);
        BigDecimal maxRecordedHumidity = readingRepository.findMaxHumidityByPointCodeAndTimeRange(
                alert.getPointCode(), interruptionStartTime, interruptionEndTime);
        BigDecimal minRecordedHumidity = readingRepository.findMinHumidityByPointCodeAndTimeRange(
                alert.getPointCode(), interruptionStartTime, interruptionEndTime);

        if (request.getDecision() == DisposalDecision.REINSPECTION_PASSED) {
            boolean withinDuration = interruptionDuration.compareTo(maxTolerance) <= 0;
            boolean withinTemp = true;
            if (maxRecordedTemp != null && extremeTemp != null) {
                withinTemp = maxRecordedTemp.compareTo(extremeTemp) <= 0;
            }
            if (!withinDuration || !withinTemp) {
                log.warn("批次{}复检通过但检查不通过: withinDuration={}, withinTemp={}",
                        batch.getBatchNo(), withinDuration, withinTemp);
            }
            batch.setStatus(BatchStatus.NORMAL);
        } else if (request.getDecision() == DisposalDecision.REJECTED) {
            batch.setStatus(BatchStatus.COLD_CHAIN_REJECTED);
            batch.setAvailableQuantity(0);
            batch.setPreoccupiedQuantity(0);
        }

        drugBatchRepository.save(batch);

        ColdChainDisposalRecord record = new ColdChainDisposalRecord();
        record.setPointCode(alert.getPointCode());
        record.setAlertId(alert.getId());
        record.setDrugCode(batch.getDrugCode());
        record.setDrugName(batch.getDrugName());
        record.setBatchNo(batch.getBatchNo());
        record.setAffectedQuantity(batch.getTotalQuantity());
        record.setInterruptionStartTime(interruptionStartTime);
        record.setInterruptionEndTime(alert.getResolvedTime());
        record.setInterruptionDurationMinutes(interruptionDuration);
        record.setMaxToleranceMinutes(maxTolerance);
        record.setMaxRecordedTemperature(maxRecordedTemp);
        record.setMinRecordedTemperature(minRecordedTemp);
        record.setDrugExtremeTemperature(extremeTemp);

        BigDecimal durationCheck = interruptionDuration.compareTo(maxTolerance) <= 0
                ? interruptionDuration : maxTolerance;
        boolean tempCheck = true;
        if (maxRecordedTemp != null && extremeTemp != null) {
            tempCheck = maxRecordedTemp.compareTo(extremeTemp) <= 0;
        }
        record.setWithinTolerance(interruptionDuration.compareTo(maxTolerance) <= 0 && tempCheck);

        record.setDecision(request.getDecision());
        record.setDisposedBy(request.getDisposedBy());
        record.setDisposalRemark(request.getDisposalRemark());
        record.setDisposedAt(LocalDateTime.now());

        record = disposalRecordRepository.save(record);

        log.info("冷链处置完成: batchNo={}, decision={}, disposedBy={}, withinTolerance={}",
                batch.getBatchNo(), request.getDecision(), request.getDisposedBy(), record.getWithinTolerance());

        return record;
    }

    public BatchEnvironmentHistoryDTO getBatchEnvironmentHistory(String batchNo) {
        List<DrugBatch> batches = drugBatchRepository.findAllByBatchNo(batchNo);
        if (batches.isEmpty()) {
            throw new ResourceNotFoundException("批次不存在: " + batchNo);
        }

        DrugBatch sample = batches.get(0);
        BatchEnvironmentHistoryDTO dto = new BatchEnvironmentHistoryDTO();
        dto.setBatchNo(batchNo);
        dto.setDrugCode(sample.getDrugCode());
        dto.setDrugName(sample.getDrugName());

        List<MonitoringPoint> points = monitoringPointRepository.findByBoundBatchNo(batchNo);
        List<BatchEnvironmentHistoryDTO.EnvironmentAbnormalRecord> records = new ArrayList<>();

        for (MonitoringPoint point : points) {
            List<AlertStatus> allStatuses = Arrays.asList(AlertStatus.values());
            List<AlertEvent> alerts = alertEventRepository.findByPointCodeAndStatusInAndTimeRange(
                    point.getPointCode(), allStatuses,
                    LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now());

            for (AlertEvent alert : alerts) {
                if (alert.getParentAlertId() != null) {
                    continue;
                }

                List<AlertEvent> chain = buildFullChain(alert);
                AlertEvent highest = chain.stream()
                        .max((a, b) -> Integer.compare(a.getAlertLevel().getSeverity(), b.getAlertLevel().getSeverity()))
                        .orElse(alert);

                LocalDateTime startTime = alert.getFirstTriggerTime();
                LocalDateTime endTime = alert.getResolvedTime() != null
                        ? alert.getResolvedTime() : LocalDateTime.now();

                BatchEnvironmentHistoryDTO.EnvironmentAbnormalRecord record =
                        new BatchEnvironmentHistoryDTO.EnvironmentAbnormalRecord();
                record.setAlertId(alert.getId());
                record.setPointCode(point.getPointCode());
                record.setPointName(point.getPointName());
                record.setAlertLevel(highest.getAlertLevel().name());
                record.setAlertLevelDescription(highest.getAlertLevel().getDescription());
                record.setStartTime(startTime);
                record.setEndTime(alert.getResolvedTime());
                record.setMaxTemperature(readingRepository.findMaxTemperatureByPointCodeAndTimeRange(
                        point.getPointCode(), startTime, endTime));
                record.setMinTemperature(readingRepository.findMinTemperatureByPointCodeAndTimeRange(
                        point.getPointCode(), startTime, endTime));
                record.setMaxHumidity(readingRepository.findMaxHumidityByPointCodeAndTimeRange(
                        point.getPointCode(), startTime, endTime));
                record.setMinHumidity(readingRepository.findMinHumidityByPointCodeAndTimeRange(
                        point.getPointCode(), startTime, endTime));
                record.setDurationMinutes(Duration.between(startTime, endTime).toMinutes());

                List<ColdChainDisposalRecord> disposals = disposalRecordRepository.findByBatchNo(batchNo);
                if (!disposals.isEmpty()) {
                    ColdChainDisposalRecord disposal = disposals.get(0);
                    record.setDisposalDecision(disposal.getDecision().name());
                    record.setDisposalDecisionDescription(disposal.getDecision().getDescription());
                }

                records.add(record);
            }
        }

        dto.setAbnormalRecords(records);
        return dto;
    }

    private List<AlertEvent> buildFullChain(AlertEvent root) {
        List<AlertEvent> chain = new ArrayList<>();
        chain.add(root);
        collectChainChildren(root.getId(), chain);
        return chain;
    }

    private void collectChainChildren(Long parentId, List<AlertEvent> chain) {
        List<AlertEvent> children = alertEventRepository.findByParentAlertId(parentId);
        for (AlertEvent child : children) {
            chain.add(child);
            collectChainChildren(child.getId(), chain);
        }
    }

    public List<ColdChainDisposalRecord> getDisposalRecordsByAlertId(Long alertId) {
        return disposalRecordRepository.findByAlertId(alertId);
    }

    public List<ColdChainDisposalRecord> getDisposalRecordsByBatchNo(String batchNo) {
        return disposalRecordRepository.findByBatchNo(batchNo);
    }
}
