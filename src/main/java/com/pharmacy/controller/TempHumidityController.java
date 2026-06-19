package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.TempHumidityReadingDTO;
import com.pharmacy.dto.coldchain.TempHumidityReportDTO;
import com.pharmacy.service.coldchain.TempHumidityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/temp-humidity")
@RequiredArgsConstructor
public class TempHumidityController {

    private final TempHumidityService tempHumidityService;

    @PostMapping("/report")
    public ApiResponse<TempHumidityReadingDTO> reportReading(@Valid @RequestBody TempHumidityReportDTO reportDTO) {
        return ApiResponse.success("数据上报成功", tempHumidityService.reportReading(reportDTO));
    }

    @PostMapping("/report-batch")
    public ApiResponse<Integer> reportReadingsBatch(@Valid @RequestBody List<TempHumidityReportDTO> reports) {
        int count = 0;
        for (TempHumidityReportDTO report : reports) {
            tempHumidityService.reportReading(report);
            count++;
        }
        return ApiResponse.success("批量上报成功，共" + count + "条", count);
    }

    @GetMapping("/{pointCode}/recent")
    public ApiResponse<List<TempHumidityReadingDTO>> getRecentReadings(
            @PathVariable String pointCode,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(tempHumidityService.getRecentReadings(pointCode, limit));
    }

    @GetMapping("/{pointCode}/range")
    public ApiResponse<List<TempHumidityReadingDTO>> getReadingsByTimeRange(
            @PathVariable String pointCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(tempHumidityService.getReadingsByTimeRange(pointCode, startTime, endTime));
    }
}
