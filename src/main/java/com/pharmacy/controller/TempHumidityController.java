package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.TempHumidityReadingDTO;
import com.pharmacy.dto.coldchain.TempHumidityReportDTO;
import com.pharmacy.service.coldchain.TempHumidityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "温湿度数据上报", description = "传感器数据上报和历史数据查询接口")
@RestController
@RequestMapping("/temp-humidity")
@RequiredArgsConstructor
public class TempHumidityController {

    private final TempHumidityService tempHumidityService;

    @Operation(summary = "上报温湿度读数", description = "模拟传感器定时上报温湿度数据，系统自动判定是否超标并触发告警")
    @PostMapping("/report")
    public ApiResponse<TempHumidityReadingDTO> reportReading(@Valid @RequestBody TempHumidityReportDTO reportDTO) {
        return ApiResponse.success("数据上报成功", tempHumidityService.reportReading(reportDTO));
    }

    @Operation(summary = "批量上报温湿度读数", description = "支持一次上报多个监控点的读数")
    @PostMapping("/report-batch")
    public ApiResponse<Integer> reportReadingsBatch(@Valid @RequestBody List<TempHumidityReportDTO> reports) {
        int count = 0;
        for (TempHumidityReportDTO report : reports) {
            tempHumidityService.reportReading(report);
            count++;
        }
        return ApiResponse.success("批量上报成功，共" + count + "条", count);
    }

    @Operation(summary = "查询最近N条读数", description = "按监控点查询最近的N条温湿度读数记录")
    @GetMapping("/{pointCode}/recent")
    public ApiResponse<List<TempHumidityReadingDTO>> getRecentReadings(
            @PathVariable String pointCode,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(tempHumidityService.getRecentReadings(pointCode, limit));
    }

    @Operation(summary = "按时间段查询温湿度曲线", description = "查询指定监控点在时间段内的全部温湿度数据，用于绘制趋势曲线")
    @GetMapping("/{pointCode}/range")
    public ApiResponse<List<TempHumidityReadingDTO>> getReadingsByTimeRange(
            @PathVariable String pointCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(tempHumidityService.getReadingsByTimeRange(pointCode, startTime, endTime));
    }
}
