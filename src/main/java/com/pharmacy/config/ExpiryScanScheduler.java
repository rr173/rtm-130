package com.pharmacy.config;

import com.pharmacy.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryScanScheduler {

    private final BatchService batchService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void scanExpiryDaily() {
        log.info("开始执行每日效期扫描定时任务");
        try {
            batchService.scanAndUpdateExpiryStatus();
            log.info("每日效期扫描任务执行完成");
        } catch (Exception e) {
            log.error("每日效期扫描任务执行失败", e);
        }
    }
}
