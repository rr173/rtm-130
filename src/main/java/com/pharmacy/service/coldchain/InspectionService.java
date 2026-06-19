package com.pharmacy.service.coldchain;

import com.pharmacy.config.ColdChainConfig;
import com.pharmacy.dto.coldchain.InspectionReportDTO;
import com.pharmacy.dto.coldchain.InspectionReportItemDTO;
import com.pharmacy.entity.InspectionReport;
import com.pharmacy.entity.InspectionReportItem;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.entity.TempHumidityReading;
import com.pharmacy.enums.InspectionAbnormalType;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.InspectionReportRepository;
import com.pharmacy.repository.MonitoringPointRepository;
import com.pharmacy.repository.TempHumidityReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionReportRepository inspectionReportRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final TempHumidityReadingRepository readingRepository;
    private final ColdChainConfig coldChainConfig;

    @Transactional
    public InspectionReportDTO executeDailyInspection() {
        log.info("开始执行每日自动巡检...");
        ColdChainConfig.InspectionConfig config = coldChainConfig.getInspection();

        InspectionReport report = new InspectionReport();
        report.setInspectionType("DAILY_AUTO");
        report.setInspectionTime(LocalDateTime.now());

        List<MonitoringPoint> enabledPoints = monitoringPointRepository.findByEnabledTrue();
        report.setTotalPoints(enabledPoints.size());

        int normalCount = 0;
        int abnormalCount = 0;

        for (MonitoringPoint point : enabledPoints) {
            InspectionReportItem item = inspectPoint(point, config);
            report.addItem(item);

            if (Boolean.TRUE.equals(item.getIsNormal())) {
                normalCount++;
            } else {
                abnormalCount++;
            }
        }

        report.setNormalPoints(normalCount);
        report.setAbnormalPoints(abnormalCount);
        report.setSummary("巡检完成：共" + report.getTotalPoints() + "个监控点，正常"
                + normalCount + "个，异常" + abnormalCount + "个");

        report = inspectionReportRepository.save(report);
        log.info("每日巡检完成: reportId={}, total={}, normal={}, abnormal={}",
                report.getId(), report.getTotalPoints(), normalCount, abnormalCount);

        return convertToDTO(report, true);
    }

    private InspectionReportItem inspectPoint(MonitoringPoint point, ColdChainConfig.InspectionConfig config) {
        InspectionReportItem item = new InspectionReportItem();
        item.setPointCode(point.getPointCode());
        item.setPointName(point.getPointName());
        List<InspectionAbnormalType> abnormalTypes = new ArrayList<>();
        StringBuilder remark = new StringBuilder();

        if (!Boolean.TRUE.equals(point.getOnline())) {
            abnormalTypes.add(InspectionAbnormalType.OFFLINE);
            remark.append("传感器离线; ");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(config.getFrequencyCheckHours());
        List<TempHumidityReading> readings = readingRepository.findByPointCodeAndCollectTimeBetween(
                point.getPointCode(), startTime, now);

        int expectedCount = (int) Math.ceil((double) config.getFrequencyCheckHours() * 60 / point.getReportIntervalMinutes());
        int actualCount = readings.size();
        item.setExpectedReadingCount(expectedCount);
        item.setActualReadingCount(actualCount);

        if (expectedCount > 0) {
            BigDecimal ratio = BigDecimal.valueOf(actualCount)
                    .divide(BigDecimal.valueOf(expectedCount), 4, RoundingMode.HALF_UP);
            item.setFrequencyRatio(ratio);

            if (ratio.compareTo(BigDecimal.valueOf(config.getFrequencyPassRatio())) < 0) {
                abnormalTypes.add(InspectionAbnormalType.FREQUENCY_ABNORMAL);
                remark.append("上报频率不达标: 实际").append(actualCount).append("次/期望")
                        .append(expectedCount).append("次, 达标率")
                        .append(ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                        .append("%; ");
            }
        }

        List<TempHumidityReading> recentReadings = readingRepository.findRecentByPointCode(
                point.getPointCode(), PageRequest.of(0, config.getTrendCheckReadings()));
        if (recentReadings.size() >= config.getTrendCheckReadings()) {
            List<TempHumidityReading> sortedReadings = recentReadings.stream()
                    .sorted((a, b) -> a.getCollectTime().compareTo(b.getCollectTime()))
                    .collect(Collectors.toList());

            boolean isMonotonicUp = true;
            boolean isMonotonicDown = true;

            for (int i = 1; i < sortedReadings.size(); i++) {
                BigDecimal prevTemp = sortedReadings.get(i - 1).getTemperature();
                BigDecimal currTemp = sortedReadings.get(i).getTemperature();
                if (currTemp.compareTo(prevTemp) <= 0) {
                    isMonotonicUp = false;
                }
                if (currTemp.compareTo(prevTemp) >= 0) {
                    isMonotonicDown = false;
                }
            }

            BigDecimal firstTemp = sortedReadings.get(0).getTemperature();
            BigDecimal lastTemp = sortedReadings.get(sortedReadings.size() - 1).getTemperature();
            BigDecimal diff = lastTemp.subtract(firstTemp).abs();

            item.setFirstTemperature(firstTemp);
            item.setLastTemperature(lastTemp);
            item.setTemperatureDifference(diff);

            if (isMonotonicUp && diff.compareTo(config.getTrendTempDiffThreshold()) > 0) {
                abnormalTypes.add(InspectionAbnormalType.TREND_UP_ABNORMAL);
                remark.append("温度持续上升: 首").append(firstTemp).append("℃→尾").append(lastTemp)
                        .append("℃, 差值").append(diff).append("℃; ");
            } else if (isMonotonicDown && diff.compareTo(config.getTrendTempDiffThreshold()) > 0) {
                abnormalTypes.add(InspectionAbnormalType.TREND_DOWN_ABNORMAL);
                remark.append("温度持续下降: 首").append(firstTemp).append("℃→尾").append(lastTemp)
                        .append("℃, 差值").append(diff).append("℃; ");
            }
        }

        item.setAbnormalTypes(abnormalTypes);
        item.setIsNormal(abnormalTypes.isEmpty());
        if (abnormalTypes.isEmpty()) {
            remark.append("正常");
        }
        item.setRemark(remark.toString());

        return item;
    }

    public List<InspectionReportDTO> getInspectionReportList() {
        return inspectionReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> convertToDTO(r, false))
                .collect(Collectors.toList());
    }

    public InspectionReportDTO getInspectionReportDetail(Long reportId) {
        InspectionReport report = inspectionReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("巡检报告不存在: " + reportId));
        return convertToDTO(report, true);
    }

    private InspectionReportDTO convertToDTO(InspectionReport report, boolean includeItems) {
        InspectionReportDTO dto = new InspectionReportDTO();
        dto.setId(report.getId());
        dto.setInspectionType(report.getInspectionType());
        dto.setInspectionTime(report.getInspectionTime());
        dto.setTotalPoints(report.getTotalPoints());
        dto.setNormalPoints(report.getNormalPoints());
        dto.setAbnormalPoints(report.getAbnormalPoints());
        dto.setSummary(report.getSummary());
        dto.setCreatedAt(report.getCreatedAt());

        if (includeItems && report.getItems() != null) {
            dto.setItems(report.getItems().stream()
                    .map(this::convertItemToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private InspectionReportItemDTO convertItemToDTO(InspectionReportItem item) {
        InspectionReportItemDTO dto = new InspectionReportItemDTO();
        dto.setId(item.getId());
        dto.setPointCode(item.getPointCode());
        dto.setPointName(item.getPointName());
        dto.setIsNormal(item.getIsNormal());
        dto.setAbnormalTypes(item.getAbnormalTypes());
        dto.setAbnormalTypeDescriptions(item.getAbnormalTypes().stream()
                .map(InspectionAbnormalType::getDescription)
                .collect(Collectors.toList()));
        dto.setExpectedReadingCount(item.getExpectedReadingCount());
        dto.setActualReadingCount(item.getActualReadingCount());
        dto.setFrequencyRatio(item.getFrequencyRatio());
        dto.setFirstTemperature(item.getFirstTemperature());
        dto.setLastTemperature(item.getLastTemperature());
        dto.setTemperatureDifference(item.getTemperatureDifference());
        dto.setRemark(item.getRemark());
        return dto;
    }
}
