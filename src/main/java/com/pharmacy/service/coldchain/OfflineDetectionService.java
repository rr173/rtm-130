package com.pharmacy.service.coldchain;

import com.pharmacy.config.ColdChainConfig;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.entity.TempHumidityReading;
import com.pharmacy.repository.MonitoringPointRepository;
import com.pharmacy.repository.TempHumidityReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineDetectionService {

    private final MonitoringPointRepository monitoringPointRepository;
    private final TempHumidityReadingRepository readingRepository;
    private final AlertService alertService;
    private final ColdChainConfig coldChainConfig;

    @Transactional
    public void checkOfflineMonitoringPoints() {
        List<MonitoringPoint> enabledPoints = monitoringPointRepository.findByEnabledTrue();
        LocalDateTime now = LocalDateTime.now();
        int missedThreshold = coldChainConfig.getOfflineDetection().getMissedIntervalsThreshold();

        for (MonitoringPoint point : enabledPoints) {
            Optional<TempHumidityReading> lastReading = readingRepository.findTopByPointCodeOrderByCollectTimeDesc(
                    point.getPointCode());

            LocalDateTime lastReportTime = lastReading.map(TempHumidityReading::getCollectTime).orElse(null);
            if (lastReportTime != null) {
                point.setLastReportTime(lastReportTime);
            } else if (point.getLastReportTime() != null) {
                lastReportTime = point.getLastReportTime();
            }

            if (lastReportTime == null) {
                if (point.getCreatedAt() != null) {
                    lastReportTime = point.getCreatedAt();
                } else {
                    continue;
                }
            }

            long minutesSinceLastReport = java.time.Duration.between(lastReportTime, now).toMinutes();
            long thresholdMinutes = (long) point.getReportIntervalMinutes() * missedThreshold;

            if (minutesSinceLastReport >= thresholdMinutes) {
                if (Boolean.TRUE.equals(point.getOnline())) {
                    point.setOnline(false);
                    monitoringPointRepository.save(point);
                    log.warn("监控点标记为离线: pointCode={}, 距上次上报={}分钟, 阈值={}分钟",
                            point.getPointCode(), minutesSinceLastReport, thresholdMinutes);
                }
                alertService.createOfflineAlert(point, lastReportTime);
            }
        }
    }

    @Transactional
    public void markPointOnline(String pointCode, LocalDateTime reportTime) {
        monitoringPointRepository.findByPointCode(pointCode).ifPresent(point -> {
            boolean wasOffline = !Boolean.TRUE.equals(point.getOnline());
            point.setOnline(true);
            point.setLastReportTime(reportTime);
            monitoringPointRepository.save(point);

            if (wasOffline) {
                log.info("监控点恢复在线: pointCode={}, reportTime={}", pointCode, reportTime);
                alertService.resolveOfflineAlerts(pointCode, reportTime);
            }
        });
    }
}
