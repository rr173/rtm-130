package com.pharmacy.config;

import com.pharmacy.entity.DispensingWindow;
import com.pharmacy.service.DispenseQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispenseTimeoutScheduler {

    private final DispenseQueueService dispenseQueueService;

    @Scheduled(fixedRate = 60000)
    public void checkDispenseTimeout() {
        try {
            List<DispensingWindow> timedOutWindows = dispenseQueueService.checkAndReturnTimeout();
            if (!timedOutWindows.isEmpty()) {
                log.info("配药超时检查完成，共{}个窗口处方被退回", timedOutWindows.size());
            }
        } catch (Exception e) {
            log.error("配药超时检查任务执行失败", e);
        }
    }
}
