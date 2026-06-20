package com.pharmacy.service.reviewrule;

import com.pharmacy.entity.reviewrule.ReviewRuleConfig;
import com.pharmacy.enums.ReviewResultType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class ReviewRuleConfigHolder {

    @Getter
    private volatile ReviewRuleConfig currentConfig;

    private final AtomicReference<ReviewRuleConfig> configRef = new AtomicReference<>();

    public void updateConfig(ReviewRuleConfig config) {
        this.configRef.set(config);
        this.currentConfig = config;
        log.info("审核规则配置已热更新至版本: {}, 极量倍数: {}, 重复用药窗口: {}小时, " +
                        "严重禁忌: {}, 中度禁忌: {}, 轻度禁忌: {}",
                config.getVersion(),
                config.getMaxSingleDoseMultiplier(),
                config.getDuplicateMedicationWindowHours(),
                config.getSevereContraindicationAction(),
                config.getModerateContraindicationAction(),
                config.getMildContraindicationAction());
    }

    public BigDecimal getMaxSingleDoseMultiplier() {
        return configRef.get().getMaxSingleDoseMultiplier();
    }

    public int getDuplicateMedicationWindowHours() {
        return configRef.get().getDuplicateMedicationWindowHours();
    }

    public ReviewResultType getContraindicationAction(com.pharmacy.enums.ContraindicationLevel level) {
        ReviewRuleConfig config = configRef.get();
        return switch (level) {
            case SEVERE -> config.getSevereContraindicationAction();
            case MODERATE -> config.getModerateContraindicationAction();
            case MILD -> config.getMildContraindicationAction();
        };
    }

    public int getCurrentVersion() {
        return configRef.get().getVersion();
    }
}
