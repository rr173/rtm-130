package com.pharmacy.config;

import com.pharmacy.service.coldchain.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertUpgradeScheduler {

    private final AlertService alertService;

    @Scheduled(fixedDelayString = "${cold-chain.alert-upgrade-interval:60000}")
    public void checkAlertUpgrades() {
        try {
            log.debug("开始检查告警升级...");
            alertService.checkAndUpgradeAlerts();
        } catch (Exception e) {
            log.error("告警升级检查失败", e);
        }
    }
}
