package com.pharmacy.config;

import com.pharmacy.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationTimeoutScheduler {

    private final ConsultationService consultationService;

    @Scheduled(fixedRate = 60000)
    public void checkConsultationTimeout() {
        try {
            consultationService.processTimeoutOpinions();
        } catch (Exception e) {
            log.error("会诊超时检查任务执行失败", e);
        }
    }
}
