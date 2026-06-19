package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.AlertEventDTO;
import com.pharmacy.entity.AlertEvent;
import com.pharmacy.enums.AlertLevel;
import com.pharmacy.service.coldchain.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/active")
    public ApiResponse<List<AlertEventDTO>> getActiveAlerts(
            @RequestParam(required = false) AlertLevel level) {
        return ApiResponse.success(alertService.getActiveAlertsByLevel(level));
    }

    @GetMapping("/{alertId}")
    public ApiResponse<AlertEventDTO> getAlertById(@PathVariable Long alertId) {
        return ApiResponse.success(alertService.getAlertById(alertId));
    }

    @GetMapping("/{alertId}/chain")
    public ApiResponse<List<AlertEvent>> getAlertChain(@PathVariable Long alertId) {
        return ApiResponse.success(alertService.getAlertChain(alertId));
    }

    @GetMapping("/by-point/{pointCode}")
    public ApiResponse<List<AlertEventDTO>> getAlertsByTimeRange(
            @PathVariable String pointCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(alertService.getAlertsByTimeRange(pointCode, startTime, endTime));
    }

    @PostMapping("/check-upgrades")
    public ApiResponse<String> checkUpgrades() {
        alertService.checkAndUpgradeAlerts();
        return ApiResponse.success("告警升级检查执行完成", null);
    }
}
