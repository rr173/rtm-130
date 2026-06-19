package com.pharmacy.config;

import com.pharmacy.service.coldchain.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionScheduler {

    private final InspectionService inspectionService;

    @Scheduled(cron = "${cold-chain.inspection.cron-expression:0 0 8 * * ?}")
    public void executeDailyInspection() {
        try {
            log.info("开始执行每日自动巡检任务...");
            inspectionService.executeDailyInspection();
        } catch (Exception e) {
            log.error("每日自动巡检任务执行失败", e);
        }
    }
}
