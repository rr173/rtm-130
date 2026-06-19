package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.AlertEventDTO;
import com.pharmacy.entity.AlertEvent;
import com.pharmacy.enums.AlertLevel;
import com.pharmacy.service.coldchain.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "告警管理", description = "告警事件查询、告警升级和恢复管理")
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "查询未关闭的告警", description = "按告警级别筛选未关闭的告警列表，不传级别则返回全部")
    @GetMapping("/active")
    public ApiResponse<List<AlertEventDTO>> getActiveAlerts(
            @RequestParam(required = false) AlertLevel level) {
        return ApiResponse.success(alertService.getActiveAlertsByLevel(level));
    }

    @Operation(summary = "按ID查询告警详情", description = "查询单个告警事件的完整信息")
    @GetMapping("/{alertId}")
    public ApiResponse<AlertEventDTO> getAlertById(@PathVariable Long alertId) {
        return ApiResponse.success(alertService.getAlertById(alertId));
    }

    @Operation(summary = "查询告警链", description = "查询某个原始告警及其所有升级后的告警事件")
    @GetMapping("/{alertId}/chain")
    public ApiResponse<List<AlertEvent>> getAlertChain(@PathVariable Long alertId) {
        return ApiResponse.success(alertService.getAlertChain(alertId));
    }

    @Operation(summary = "按监控点和时间段查询告警", description = "查询指定监控点在时间段内产生的所有告警")
    @GetMapping("/by-point/{pointCode}")
    public ApiResponse<List<AlertEventDTO>> getAlertsByTimeRange(
            @PathVariable String pointCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(alertService.getAlertsByTimeRange(pointCode, startTime, endTime));
    }

    @Operation(summary = "手动触发告警升级检查", description = "立即执行一次告警升级检查（黄色→橙色→红色），通常由定时任务自动执行")
    @PostMapping("/check-upgrades")
    public ApiResponse<String> checkUpgrades() {
        alertService.checkAndUpgradeAlerts();
        return ApiResponse.success("告警升级检查执行完成", null);
    }
}
