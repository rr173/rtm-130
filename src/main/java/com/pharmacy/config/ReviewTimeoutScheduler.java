package com.pharmacy.config;

import com.pharmacy.entity.Prescription;
import com.pharmacy.service.PharmacistReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewTimeoutScheduler {

    private final PharmacistReviewService pharmacistReviewService;

    @Scheduled(fixedRate = 60000)
    public void checkReviewTimeout() {
        try {
            List<Prescription> releasedPrescriptions = pharmacistReviewService.releaseTimeoutReviews();
            if (!releasedPrescriptions.isEmpty()) {
                log.info("审方超时检查完成，共{}张处方被释放回待办池", releasedPrescriptions.size());
            }
        } catch (Exception e) {
            log.error("审方超时检查任务执行失败", e);
        }
    }
}
