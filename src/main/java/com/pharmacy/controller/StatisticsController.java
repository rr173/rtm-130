package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.DailyDispenseStatisticsDTO;
import com.pharmacy.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final QueryService queryService;

    @GetMapping("/daily-dispense")
    public ApiResponse<List<DailyDispenseStatisticsDTO>> getDailyDispenseStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<DailyDispenseStatisticsDTO> result =
                queryService.getDailyDispenseStatistics(startDate, endDate);
        return ApiResponse.success(result);
    }

    @GetMapping("/dispense-ranking")
    public ApiResponse<List<Map<String, Object>>> getDrugDispenseRanking(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> result =
                queryService.getDrugDispenseRanking(startDate, endDate, limit);
        return ApiResponse.success(result);
    }
}
