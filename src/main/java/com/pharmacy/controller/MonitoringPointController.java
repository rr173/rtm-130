package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.MonitoringPointDTO;
import com.pharmacy.service.coldchain.MonitoringPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monitoring-points")
@RequiredArgsConstructor
public class MonitoringPointController {

    private final MonitoringPointService monitoringPointService;

    @GetMapping
    public ApiResponse<List<MonitoringPointDTO>> getAllMonitoringPoints() {
        return ApiResponse.success(monitoringPointService.getAllMonitoringPoints());
    }

    @GetMapping("/{pointCode}")
    public ApiResponse<MonitoringPointDTO> getMonitoringPoint(@PathVariable String pointCode) {
        return ApiResponse.success(monitoringPointService.getMonitoringPointByCode(pointCode));
    }

    @PostMapping
    public ApiResponse<MonitoringPointDTO> createMonitoringPoint(@RequestBody MonitoringPointDTO dto) {
        return ApiResponse.success("监控点创建成功", monitoringPointService.createMonitoringPoint(dto));
    }

    @PostMapping("/{pointCode}/bind-batches")
    public ApiResponse<MonitoringPointDTO> bindBatches(
            @PathVariable String pointCode,
            @RequestBody List<String> batchNos) {
        return ApiResponse.success("批次绑定成功", monitoringPointService.bindBatches(pointCode, batchNos));
    }

    @PostMapping("/{pointCode}/unbind-batches")
    public ApiResponse<MonitoringPointDTO> unbindBatches(
            @PathVariable String pointCode,
            @RequestBody List<String> batchNos) {
        return ApiResponse.success("批次解绑成功", monitoringPointService.unbindBatches(pointCode, batchNos));
    }

    @GetMapping("/by-batch/{batchNo}")
    public ApiResponse<List<MonitoringPointDTO>> getMonitoringPointsByBatch(@PathVariable String batchNo) {
        return ApiResponse.success(monitoringPointService.getMonitoringPointsByBatchNo(batchNo));
    }
}
