package com.pharmacy.config;

import com.pharmacy.enums.DrugCategory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "cold-chain")
public class ColdChainConfig {

    private Map<String, CategoryTolerance> categoryTolerances = new HashMap<>();

    private AlertThresholds alertThresholds = new AlertThresholds();

    private UpgradeThresholds upgradeThresholds = new UpgradeThresholds();

    @Data
    public static class CategoryTolerance {
        private BigDecimal maxToleranceMinutes;
        private BigDecimal extremeTemperature;
    }

    @Data
    public static class AlertThresholds {
        private BigDecimal yellowTempDeviation = new BigDecimal("2.00");
        private BigDecimal yellowHumidityDeviation = new BigDecimal("5.00");
        private BigDecimal orangeTempDeviation = new BigDecimal("5.00");
        private BigDecimal orangeHumidityDeviation = new BigDecimal("10.00");
        private BigDecimal redTempDeviation = new BigDecimal("5.00");
        private BigDecimal redHumidityDeviation = new BigDecimal("20.00");
    }

    @Data
    public static class UpgradeThresholds {
        private int yellowToOrangeMinutes = 10;
        private int orangeToRedMinutes = 30;
    }

    public BigDecimal getMaxToleranceMinutes(DrugCategory category) {
        CategoryTolerance tolerance = categoryTolerances.get(category.name());
        if (tolerance != null && tolerance.getMaxToleranceMinutes() != null) {
            return tolerance.getMaxToleranceMinutes();
        }
        if (category == DrugCategory.VACCINE) {
            return new BigDecimal("30");
        }
        return new BigDecimal("60");
    }

    public BigDecimal getExtremeTemperature(DrugCategory category) {
        CategoryTolerance tolerance = categoryTolerances.get(category.name());
        if (tolerance != null && tolerance.getExtremeTemperature() != null) {
            return tolerance.getExtremeTemperature();
        }
        if (category == DrugCategory.VACCINE) {
            return new BigDecimal("10.00");
        }
        if (category == DrugCategory.BIOLOGICAL || category == DrugCategory.INSULIN) {
            return new BigDecimal("15.00");
        }
        return new BigDecimal("25.00");
    }
}
