package com.pharmacy.config;

import com.pharmacy.service.coldchain.OfflineDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineDetectionScheduler {

    private final OfflineDetectionService offlineDetectionService;

    @Scheduled(fixedDelayString = "${cold-chain.offline-detection.check-interval-ms:60000}")
    public void checkOfflineSensors() {
        try {
            log.debug("开始检查传感器离线状态...");
            offlineDetectionService.checkOfflineMonitoringPoints();
        } catch (Exception e) {
            log.error("传感器离线检测失败", e);
        }
    }
}
