package com.pharmacy.service.coldchain;

import com.pharmacy.config.ColdChainConfig;
import com.pharmacy.dto.coldchain.TempHumidityReadingDTO;
import com.pharmacy.dto.coldchain.TempHumidityReportDTO;
import com.pharmacy.entity.AlertEvent;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.entity.TempHumidityReading;
import com.pharmacy.enums.AlertLevel;
import com.pharmacy.enums.AlertStatus;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.AlertEventRepository;
import com.pharmacy.repository.MonitoringPointRepository;
import com.pharmacy.repository.TempHumidityReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TempHumidityService {

    private final TempHumidityReadingRepository readingRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final AlertEventRepository alertEventRepository;
    private final ColdChainConfig coldChainConfig;
    private final AlertService alertService;
    private final OfflineDetectionService offlineDetectionService;

    @Transactional
    public TempHumidityReadingDTO reportReading(TempHumidityReportDTO reportDTO) {
        MonitoringPoint point = monitoringPointRepository.findByPointCode(reportDTO.getPointCode())
                .orElseThrow(() -> new ResourceNotFoundException("监控点不存在: " + reportDTO.getPointCode()));

        boolean tempOutOfRange = !point.isTemperatureInRange(reportDTO.getTemperature());
        boolean humidityOutOfRange = !point.isHumidityInRange(reportDTO.getHumidity());

        TempHumidityReading reading = new TempHumidityReading();
        reading.setPointCode(reportDTO.getPointCode());
        reading.setTemperature(reportDTO.getTemperature());
        reading.setHumidity(reportDTO.getHumidity());
        reading.setCollectTime(reportDTO.getCollectTime());
        reading.setTempOutOfRange(tempOutOfRange);
        reading.setHumidityOutOfRange(humidityOutOfRange);
        reading.setRemark(reportDTO.getRemark());
        reading = readingRepository.save(reading);

        offlineDetectionService.markPointOnline(point.getPointCode(), reading.getCollectTime());

        if (tempOutOfRange || humidityOutOfRange) {
            handleOutOfRange(point, reading);
        } else {
            handleBackToNormal(point, reading);
        }

        return convertToDTO(reading);
    }

    private void handleOutOfRange(MonitoringPoint point, TempHumidityReading reading) {
        ColdChainConfig.AlertThresholds thresholds = coldChainConfig.getAlertThresholds();

        BigDecimal tempDeviation = calculateTempDeviation(point, reading.getTemperature());
        BigDecimal humidityDeviation = calculateHumidityDeviation(point, reading.getHumidity());

        AlertLevel level = determineAlertLevel(tempDeviation, humidityDeviation, thresholds);

        List<AlertStatus> activeStatuses = List.of(AlertStatus.ACTIVE);
        List<AlertEvent> activeAlerts = alertEventRepository.findByPointCodeAndAlertStatusIn(
                point.getPointCode(), activeStatuses);

        if (activeAlerts.isEmpty()) {
            alertService.createAlert(point, reading, level, tempDeviation, humidityDeviation);
        } else {
            AlertEvent activeAlert = activeAlerts.get(0);
            if (level.getSeverity() > activeAlert.getAlertLevel().getSeverity()) {
                alertService.upgradeAlert(activeAlert, level, reading, "读数超标程度加剧");
            } else {
                activeAlert.setLastTriggerTime(reading.getCollectTime());
                activeAlert.setTriggerTemperature(reading.getTemperature());
                activeAlert.setTriggerHumidity(reading.getHumidity());
                alertEventRepository.save(activeAlert);
            }
        }
    }

    private void handleBackToNormal(MonitoringPoint point, TempHumidityReading reading) {
        List<AlertStatus> activeStatuses = List.of(AlertStatus.ACTIVE);
        List<AlertEvent> activeAlerts = alertEventRepository.findByPointCodeAndAlertStatusIn(
                point.getPointCode(), activeStatuses);

        for (AlertEvent alert : activeAlerts) {
            alertService.resolveAlert(alert, reading.getCollectTime(), "SYSTEM", "温湿度读数恢复到合规区间");
        }
    }

    private BigDecimal calculateTempDeviation(MonitoringPoint point, BigDecimal temperature) {
        if (temperature.compareTo(point.getTempMax()) > 0) {
            return temperature.subtract(point.getTempMax());
        } else if (temperature.compareTo(point.getTempMin()) < 0) {
            return point.getTempMin().subtract(temperature);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateHumidityDeviation(MonitoringPoint point, BigDecimal humidity) {
        if (humidity.compareTo(point.getHumidityMax()) > 0) {
            return humidity.subtract(point.getHumidityMax());
        } else if (humidity.compareTo(point.getHumidityMin()) < 0) {
            return point.getHumidityMin().subtract(humidity);
        }
        return BigDecimal.ZERO;
    }

    private AlertLevel determineAlertLevel(BigDecimal tempDev, BigDecimal humidityDev,
                                            ColdChainConfig.AlertThresholds thresholds) {
        BigDecimal maxDev = tempDev.max(humidityDev);
        boolean isTempDeterminant = tempDev.compareTo(humidityDev) >= 0;

        if (isTempDeterminant) {
            if (maxDev.compareTo(thresholds.getRedTempDeviation()) > 0) {
                return AlertLevel.RED;
            } else if (maxDev.compareTo(thresholds.getYellowTempDeviation()) > 0) {
                return AlertLevel.ORANGE;
            } else {
                return AlertLevel.YELLOW;
            }
        } else {
            if (maxDev.compareTo(thresholds.getRedHumidityDeviation()) > 0) {
                return AlertLevel.RED;
            } else if (maxDev.compareTo(thresholds.getYellowHumidityDeviation()) > 0) {
                return AlertLevel.ORANGE;
            } else {
                return AlertLevel.YELLOW;
            }
        }
    }

    public List<TempHumidityReadingDTO> getRecentReadings(String pointCode, int limit) {
        if (!monitoringPointRepository.existsByPointCode(pointCode)) {
            throw new ResourceNotFoundException("监控点不存在: " + pointCode);
        }
        return readingRepository.findRecentByPointCode(pointCode, org.springframework.data.domain.PageRequest.of(0, limit)).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TempHumidityReadingDTO> getReadingsByTimeRange(String pointCode,
                                                               LocalDateTime startTime,
                                                               LocalDateTime endTime) {
        if (!monitoringPointRepository.existsByPointCode(pointCode)) {
            throw new ResourceNotFoundException("监控点不存在: " + pointCode);
        }
        return readingRepository.findByPointCodeAndCollectTimeBetween(pointCode, startTime, endTime).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private TempHumidityReadingDTO convertToDTO(TempHumidityReading reading) {
        TempHumidityReadingDTO dto = new TempHumidityReadingDTO();
        dto.setId(reading.getId());
        dto.setPointCode(reading.getPointCode());
        dto.setTemperature(reading.getTemperature());
        dto.setHumidity(reading.getHumidity());
        dto.setCollectTime(reading.getCollectTime());
        dto.setTempOutOfRange(reading.getTempOutOfRange());
        dto.setHumidityOutOfRange(reading.getHumidityOutOfRange());
        dto.setRemark(reading.getRemark());
        return dto;
    }
}
