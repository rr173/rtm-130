package com.pharmacy.service;

import com.pharmacy.dto.DailyDispenseStatisticsDTO;
import com.pharmacy.dto.DrugDTO;
import com.pharmacy.dto.InventoryLogDTO;
import com.pharmacy.entity.InventoryLog;
import com.pharmacy.enums.InventoryLogType;
import com.pharmacy.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final InventoryLogRepository inventoryLogRepository;
    private final InventoryService inventoryService;

    public List<InventoryLogDTO> getInventoryLogs(String drugCode, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() :
                LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<InventoryLog> logs;
        if (drugCode != null && !drugCode.isBlank()) {
            logs = inventoryLogRepository.findByDrugCodeAndCreatedAtBetweenOrderByCreatedAtDesc(
                    drugCode, startTime, endTime);
        } else {
            logs = inventoryLogRepository.findByLogTypeAndDateRange(
                    InventoryLogType.DISPENSE, startTime, endTime);
        }

        return logs.stream()
                .map(InventoryLogDTO::fromEntity)
                .toList();
    }

    public List<InventoryLogDTO> getInventoryLogsByType(InventoryLogType type, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() :
                LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<InventoryLog> logs = inventoryLogRepository.findByLogTypeAndDateRange(
                type, startTime, endTime);

        return logs.stream()
                .map(InventoryLogDTO::fromEntity)
                .toList();
    }

    public List<InventoryLogDTO> getInventoryLogsByPrescription(String prescriptionNo) {
        List<InventoryLog> logs = inventoryLogRepository.findByPrescriptionNoOrderByCreatedAtDesc(prescriptionNo);
        return logs.stream()
                .map(InventoryLogDTO::fromEntity)
                .toList();
    }

    public List<DailyDispenseStatisticsDTO> getDailyDispenseStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() :
                LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<InventoryLog> dispenseLogs = inventoryLogRepository.findByLogTypeAndDateRange(
                InventoryLogType.DISPENSE, startTime, endTime);

        Map<String, Map<LocalDate, Integer>> dailyStats = dispenseLogs.stream()
                .collect(Collectors.groupingBy(
                        InventoryLog::getDrugCode,
                        Collectors.groupingBy(
                                log -> log.getCreatedAt().toLocalDate(),
                                Collectors.summingInt(InventoryLog::getQuantity)
                        )
                ));

        Map<String, String> drugNameMap = dispenseLogs.stream()
                .collect(Collectors.toMap(
                        InventoryLog::getDrugCode,
                        InventoryLog::getDrugName,
                        (existing, replacement) -> existing
                ));

        List<DailyDispenseStatisticsDTO> result = new ArrayList<>();
        for (Map.Entry<String, Map<LocalDate, Integer>> drugEntry : dailyStats.entrySet()) {
            String drugCode = drugEntry.getKey();
            String drugName = drugNameMap.get(drugCode);

            for (Map.Entry<LocalDate, Integer> dateEntry : drugEntry.getValue().entrySet()) {
                DailyDispenseStatisticsDTO dto = new DailyDispenseStatisticsDTO();
                dto.setDrugCode(drugCode);
                dto.setDrugName(drugName);
                dto.setDate(dateEntry.getKey());
                dto.setTotalQuantity(dateEntry.getValue());
                result.add(dto);
            }
        }

        result.sort((a, b) -> {
            int dateCompare = b.getDate().compareTo(a.getDate());
            if (dateCompare != 0) return dateCompare;
            return b.getTotalQuantity().compareTo(a.getTotalQuantity());
        });

        return result;
    }

    public List<Map<String, Object>> getDrugDispenseRanking(LocalDate startDate, LocalDate endDate, int limit) {
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() :
                LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<Object[]> results = inventoryLogRepository.sumQuantityByDrugAndDateRange(
                InventoryLogType.DISPENSE, startTime, endTime);

        return results.stream()
                .limit(limit > 0 ? limit : 10)
                .map(row -> Map.of(
                        "drugCode", row[0],
                        "drugName", row[1],
                        "totalQuantity", row[2]
                ))
                .toList();
    }

    public DrugDTO getDrugInventory(String drugCode) {
        return DrugDTO.fromEntity(inventoryService.getDrug(drugCode));
    }

    public List<DrugDTO> getAllDrugInventories() {
        return inventoryService.getAllDrugs().stream()
                .map(DrugDTO::fromEntity)
                .toList();
    }

    public Map<String, Object> getInventorySummary() {
        List<DrugDTO> drugs = getAllDrugInventories();

        int totalAvailable = drugs.stream()
                .mapToInt(DrugDTO::getAvailableStock)
                .sum();

        int totalPreoccupied = drugs.stream()
                .mapToInt(DrugDTO::getPreoccupiedStock)
                .sum();

        int totalActual = drugs.stream()
                .mapToInt(DrugDTO::getActualStock)
                .sum();

        long lowStockCount = drugs.stream()
                .filter(d -> d.getAvailableStock() < 10)
                .count();

        long controlledDrugCount = drugs.stream()
                .filter(d -> d.getCategory().isControlled())
                .count();

        return Map.of(
                "totalDrugTypes", drugs.size(),
                "totalAvailableStock", totalAvailable,
                "totalPreoccupiedStock", totalPreoccupied,
                "totalActualStock", totalActual,
                "lowStockCount", lowStockCount,
                "controlledDrugCount", controlledDrugCount
        );
    }
}
