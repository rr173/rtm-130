package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.pharmacistreview.PharmacistPerformanceDTO;
import com.pharmacy.dto.pharmacistreview.ReviewRankingDTO;
import com.pharmacy.dto.pharmacistreview.TodoPoolStatisticsDTO;
import com.pharmacy.service.PharmacistPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pharmacist-performance")
@RequiredArgsConstructor
public class PharmacistPerformanceController {

    private final PharmacistPerformanceService pharmacistPerformanceService;

    @GetMapping("/pharmacist/{pharmacistId}")
    public ApiResponse<PharmacistPerformanceDTO> getPharmacistPerformance(
            @PathVariable String pharmacistId,
            @RequestParam(defaultValue = "today") String period) {
        PharmacistPerformanceDTO result = pharmacistPerformanceService
                .getPharmacistPerformance(pharmacistId, period);
        return ApiResponse.success(result);
    }

    @GetMapping("/all")
    public ApiResponse<List<PharmacistPerformanceDTO>> getAllPharmacistPerformance(
            @RequestParam(defaultValue = "today") String period) {
        List<PharmacistPerformanceDTO> result = pharmacistPerformanceService
                .getAllPharmacistPerformance(period);
        return ApiResponse.success(result);
    }

    @GetMapping("/ranking/count")
    public ApiResponse<ReviewRankingDTO> getRankingByReviewCount(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(defaultValue = "10") int topN) {
        List<PharmacistPerformanceDTO> rankings = pharmacistPerformanceService
                .getRankingByReviewCount(period, topN);
        ReviewRankingDTO result = new ReviewRankingDTO();
        result.setRankingType("reviewCount");
        result.setRankings(rankings);
        return ApiResponse.success(result);
    }

    @GetMapping("/ranking/speed")
    public ApiResponse<ReviewRankingDTO> getRankingBySpeed(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(defaultValue = "10") int topN) {
        List<PharmacistPerformanceDTO> rankings = pharmacistPerformanceService
                .getRankingByAverageTime(period, topN);
        ReviewRankingDTO result = new ReviewRankingDTO();
        result.setRankingType("averageSpeed");
        result.setRankings(rankings);
        return ApiResponse.success(result);
    }

    @GetMapping("/todo-pool-statistics")
    public ApiResponse<TodoPoolStatisticsDTO> getTodoPoolStatistics(
            @RequestParam(defaultValue = "today") String period) {
        TodoPoolStatisticsDTO result = pharmacistPerformanceService
                .getTodoPoolStatistics(period);
        return ApiResponse.success(result);
    }
}
