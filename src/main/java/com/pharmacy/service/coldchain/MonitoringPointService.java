package com.pharmacy.service.coldchain;

import com.pharmacy.config.ColdChainConfig;
import com.pharmacy.dto.coldchain.MonitoringPointDTO;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.entity.TempHumidityReading;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.MonitoringPointRepository;
import com.pharmacy.repository.TempHumidityReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringPointService {

    private final MonitoringPointRepository monitoringPointRepository;
    private final TempHumidityReadingRepository readingRepository;

    public List<MonitoringPointDTO> getAllMonitoringPoints() {
        return monitoringPointRepository.findByEnabledTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public MonitoringPointDTO getMonitoringPointByCode(String pointCode) {
        MonitoringPoint point = monitoringPointRepository.findByPointCode(pointCode)
                .orElseThrow(() -> new ResourceNotFoundException("监控点不存在: " + pointCode));
        return convertToDTO(point);
    }

    public MonitoringPoint getMonitoringPointEntity(String pointCode) {
        return monitoringPointRepository.findByPointCode(pointCode)
                .orElseThrow(() -> new ResourceNotFoundException("监控点不存在: " + pointCode));
    }

    @Transactional
    public MonitoringPointDTO createMonitoringPoint(MonitoringPointDTO dto) {
        if (monitoringPointRepository.existsByPointCode(dto.getPointCode())) {
            throw new IllegalArgumentException("监控点编号已存在: " + dto.getPointCode());
        }
        MonitoringPoint point = new MonitoringPoint();
        point.setPointCode(dto.getPointCode());
        point.setPointName(dto.getPointName());
        point.setPointType(dto.getPointType());
        point.setLocationDescription(dto.getLocationDescription());
        point.setTempMin(dto.getTempMin());
        point.setTempMax(dto.getTempMax());
        point.setHumidityMin(dto.getHumidityMin());
        point.setHumidityMax(dto.getHumidityMax());
        point.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        point.setReportIntervalMinutes(dto.getReportIntervalMinutes() != null ? dto.getReportIntervalMinutes() : 5);
        point.setOnline(true);
        if (dto.getBoundBatchNos() != null) {
            point.setBoundBatchNos(dto.getBoundBatchNos());
        }
        point = monitoringPointRepository.save(point);
        log.info("创建监控点成功: {}", point.getPointCode());
        return convertToDTO(point);
    }

    @Transactional
    public MonitoringPointDTO bindBatches(String pointCode, List<String> batchNos) {
        MonitoringPoint point = getMonitoringPointEntity(pointCode);
        for (String batchNo : batchNos) {
            if (!point.getBoundBatchNos().contains(batchNo)) {
                point.getBoundBatchNos().add(batchNo);
            }
        }
        point = monitoringPointRepository.save(point);
        log.info("监控点 {} 绑定批次: {}", pointCode, batchNos);
        return convertToDTO(point);
    }

    @Transactional
    public MonitoringPointDTO unbindBatches(String pointCode, List<String> batchNos) {
        MonitoringPoint point = getMonitoringPointEntity(pointCode);
        point.getBoundBatchNos().removeAll(batchNos);
        point = monitoringPointRepository.save(point);
        log.info("监控点 {} 解绑批次: {}", pointCode, batchNos);
        return convertToDTO(point);
    }

    public List<MonitoringPointDTO> getMonitoringPointsByBatchNo(String batchNo) {
        return monitoringPointRepository.findByBoundBatchNo(batchNo).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MonitoringPointDTO convertToDTO(MonitoringPoint point) {
        MonitoringPointDTO dto = new MonitoringPointDTO();
        dto.setId(point.getId());
        dto.setPointCode(point.getPointCode());
        dto.setPointName(point.getPointName());
        dto.setPointType(point.getPointType());
        dto.setPointTypeDescription(point.getPointType().getDescription());
        dto.setLocationDescription(point.getLocationDescription());
        dto.setTempMin(point.getTempMin());
        dto.setTempMax(point.getTempMax());
        dto.setHumidityMin(point.getHumidityMin());
        dto.setHumidityMax(point.getHumidityMax());
        dto.setEnabled(point.getEnabled());
        dto.setReportIntervalMinutes(point.getReportIntervalMinutes());
        dto.setOnline(point.getOnline());
        dto.setLastReportTime(point.getLastReportTime());
        dto.setBoundBatchNos(point.getBoundBatchNos());

        readingRepository.findTopByPointCodeOrderByCollectTimeDesc(point.getPointCode())
                .ifPresent(reading -> {
                    dto.setCurrentTemperature(reading.getTemperature());
                    dto.setCurrentHumidity(reading.getHumidity());
                    if (!point.getOnline()) {
                        dto.setCurrentStatus("离线");
                    } else if (reading.getTempOutOfRange() || reading.getHumidityOutOfRange()) {
                        dto.setCurrentStatus("超标");
                    } else {
                        dto.setCurrentStatus("正常");
                    }
                });

        if (dto.getCurrentStatus() == null) {
            dto.setCurrentStatus(point.getOnline() ? "正常" : "离线");
        }

        return dto;
    }
}
