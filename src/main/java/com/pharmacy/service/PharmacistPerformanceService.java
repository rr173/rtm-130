package com.pharmacy.service;

import com.pharmacy.dto.pharmacistreview.PharmacistPerformanceDTO;
import com.pharmacy.dto.pharmacistreview.TodoPoolStatisticsDTO;
import com.pharmacy.entity.Pharmacist;
import com.pharmacy.entity.PharmacistReviewRecord;
import com.pharmacy.entity.Prescription;
import com.pharmacy.enums.PharmacistReviewConclusion;
import com.pharmacy.repository.PharmacistRepository;
import com.pharmacy.repository.PharmacistReviewRecordRepository;
import com.pharmacy.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacistPerformanceService {

    private final PharmacistReviewRecordRepository reviewRecordRepository;
    private final PharmacistRepository pharmacistRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional(readOnly = true)
    public PharmacistPerformanceDTO getPharmacistPerformance(String pharmacistId, String period) {
        LocalDateTime[] timeRange = getTimeRange(period);
        return calculatePharmacistPerformance(pharmacistId, timeRange[0], timeRange[1], period);
    }

    @Transactional(readOnly = true)
    public List<PharmacistPerformanceDTO> getAllPharmacistPerformance(String period) {
        LocalDateTime[] timeRange = getTimeRange(period);
        List<Pharmacist> pharmacists = pharmacistRepository.findByActiveTrue();

        return pharmacists.stream()
                .map(p -> calculatePharmacistPerformance(p.getPharmacistId(), timeRange[0], timeRange[1], period))
                .sorted(Comparator.comparing(PharmacistPerformanceDTO::getTotalReviewCount).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PharmacistPerformanceDTO> getRankingByReviewCount(String period, int topN) {
        List<PharmacistPerformanceDTO> performances = getAllPharmacistPerformance(period);
        return performances.stream()
                .sorted(Comparator.comparing(PharmacistPerformanceDTO::getTotalReviewCount).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PharmacistPerformanceDTO> getRankingByAverageTime(String period, int topN) {
        List<PharmacistPerformanceDTO> performances = getAllPharmacistPerformance(period);
        return performances.stream()
                .filter(p -> p.getAverageReviewSeconds() != null && p.getTotalReviewCount() > 0)
                .sorted(Comparator.comparing(PharmacistPerformanceDTO::getAverageReviewSeconds))
                .limit(topN)
                .collect(Collectors.toList());
    }

    private PharmacistPerformanceDTO calculatePharmacistPerformance(
            String pharmacistId, LocalDateTime startTime, LocalDateTime endTime, String period) {

        PharmacistPerformanceDTO dto = new PharmacistPerformanceDTO();
        dto.setPharmacistId(pharmacistId);
        dto.setPeriod(period);

        pharmacistRepository.findByPharmacistId(pharmacistId)
                .ifPresent(p -> dto.setPharmacistName(p.getName()));

        List<PharmacistReviewRecord> records = reviewRecordRepository
                .findByPharmacistIdAndReviewedAtBetween(pharmacistId, startTime, endTime);

        long totalCount = records.size();
        dto.setTotalReviewCount(totalCount);

        long passedCount = records.stream()
                .filter(r -> r.getConclusion() == PharmacistReviewConclusion.PASSED)
                .count();
        dto.setPassedCount(passedCount);

        long returnedCount = records.stream()
                .filter(r -> r.getConclusion() == PharmacistReviewConclusion.RETURNED_FOR_MODIFICATION)
                .count();
        dto.setReturnedCount(returnedCount);

        long keyAttentionCount = records.stream()
                .filter(r -> r.getConclusion() == PharmacistReviewConclusion.KEY_ATTENTION)
                .count();
        dto.setKeyAttentionCount(keyAttentionCount);

        long timeoutCount = records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsTimeout()))
                .count();
        dto.setTimeoutCount(timeoutCount);

        if (totalCount > 0) {
            BigDecimal returnRate = BigDecimal.valueOf(returnedCount)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setReturnRate(returnRate);

            BigDecimal timeoutRate = BigDecimal.valueOf(timeoutCount)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setTimeoutRate(timeoutRate);

            long totalSeconds = records.stream()
                    .filter(r -> r.getReviewDurationSeconds() != null)
                    .mapToLong(PharmacistReviewRecord::getReviewDurationSeconds)
                    .sum();
            long reviewedWithDuration = records.stream()
                    .filter(r -> r.getReviewDurationSeconds() != null)
                    .count();

            if (reviewedWithDuration > 0) {
                BigDecimal avgSeconds = BigDecimal.valueOf(totalSeconds)
                        .divide(BigDecimal.valueOf(reviewedWithDuration), 2, RoundingMode.HALF_UP);
                dto.setAverageReviewSeconds(avgSeconds);
            }
        } else {
            dto.setReturnRate(BigDecimal.ZERO);
            dto.setTimeoutRate(BigDecimal.ZERO);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public TodoPoolStatisticsDTO getTodoPoolStatistics(String period) {
        LocalDateTime[] timeRange = getTimeRange(period);
        LocalDateTime startTime = timeRange[0];
        LocalDateTime endTime = timeRange[1];

        TodoPoolStatisticsDTO statistics = new TodoPoolStatisticsDTO();
        statistics.setTimePeriod(period);

        int peakCount = calculatePeakPendingCount(startTime, endTime);
        statistics.setPeakPendingCount(peakCount);

        BigDecimal avgWaitingMinutes = calculateAverageWaitingTime(startTime, endTime);
        statistics.setAverageWaitingMinutes(avgWaitingMinutes);

        List<PharmacistReviewRecord> allRecords = reviewRecordRepository
                .findAllByReviewedAtBetween(startTime, endTime);
        statistics.setTotalReviewedCount((long) allRecords.size());

        return statistics;
    }

    private int calculatePeakPendingCount(LocalDateTime startTime, LocalDateTime endTime) {
        List<Prescription> enteredPool = prescriptionRepository
                .findTodoPoolByTimeRange(startTime, endTime);

        if (enteredPool.isEmpty()) {
            return 0;
        }

        int peakCount = 0;
        int currentCount = 0;

        List<LocalDateTime> timePoints = new ArrayList<>();
        Map<LocalDateTime, Integer> deltaMap = new HashMap<>();

        for (Prescription p : enteredPool) {
            LocalDateTime enterTime = p.getCreatedAt();
            if (enterTime.isBefore(startTime)) {
                enterTime = startTime;
            }

            timePoints.add(enterTime);
            deltaMap.merge(enterTime, 1, Integer::sum);

            LocalDateTime exitTime = p.getClaimedAt();
            if (exitTime != null && exitTime.isBefore(endTime)) {
                timePoints.add(exitTime);
                deltaMap.merge(exitTime, -1, Integer::sum);
            }
        }

        timePoints.sort(LocalDateTime::compareTo);

        for (LocalDateTime time : timePoints) {
            currentCount += deltaMap.getOrDefault(time, 0);
            if (currentCount > peakCount) {
                peakCount = currentCount;
            }
        }

        return Math.max(peakCount, (int) prescriptionRepository.countTodoPoolSize());
    }

    private BigDecimal calculateAverageWaitingTime(LocalDateTime startTime, LocalDateTime endTime) {
        List<PharmacistReviewRecord> records = reviewRecordRepository
                .findAllByReviewedAtBetween(startTime, endTime);

        if (records.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalWaitingMinutes = 0;
        int count = 0;

        for (PharmacistReviewRecord record : records) {
            if (record.getClaimedAt() != null && record.getPrescription() != null) {
                Prescription prescription = record.getPrescription();
                if (prescription.getCreatedAt() != null) {
                    long minutes = ChronoUnit.MINUTES.between(
                            prescription.getCreatedAt(), record.getClaimedAt());
                    if (minutes >= 0) {
                        totalWaitingMinutes += minutes;
                        count++;
                    }
                }
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalWaitingMinutes)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime[] getTimeRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime startTime;
        LocalDateTime endTime = LocalDateTime.now();

        switch (period.toLowerCase()) {
            case "today" -> {
                startTime = today.atStartOfDay();
            }
            case "week" -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                startTime = monday.atStartOfDay();
            }
            case "month" -> {
                LocalDate firstDay = today.withDayOfMonth(1);
                startTime = firstDay.atStartOfDay();
            }
            default -> {
                startTime = today.atStartOfDay();
            }
        }

        return new LocalDateTime[]{startTime, endTime};
    }
}
