package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.MonitoringPointDTO;
import com.pharmacy.service.coldchain.MonitoringPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "监控点管理", description = "冷藏柜、阴凉柜等温湿度监控点的注册和管理")
@RestController
@RequestMapping("/monitoring-points")
@RequiredArgsConstructor
public class MonitoringPointController {

    private final MonitoringPointService monitoringPointService;

    @Operation(summary = "查询所有监控点", description = "获取所有已启用的监控点列表及其当前状态")
    @GetMapping
    public ApiResponse<List<MonitoringPointDTO>> getAllMonitoringPoints() {
        return ApiResponse.success(monitoringPointService.getAllMonitoringPoints());
    }

    @Operation(summary = "按编号查询监控点", description = "根据监控点编号查询单个监控点详情")
    @GetMapping("/{pointCode}")
    public ApiResponse<MonitoringPointDTO> getMonitoringPoint(@PathVariable String pointCode) {
        return ApiResponse.success(monitoringPointService.getMonitoringPointByCode(pointCode));
    }

    @Operation(summary = "新增监控点", description = "注册新的监控点，设置温湿度合规区间")
    @PostMapping
    public ApiResponse<MonitoringPointDTO> createMonitoringPoint(@RequestBody MonitoringPointDTO dto) {
        return ApiResponse.success("监控点创建成功", monitoringPointService.createMonitoringPoint(dto));
    }

    @Operation(summary = "绑定药品批次", description = "将指定的药品批次编号绑定到监控点")
    @PostMapping("/{pointCode}/bind-batches")
    public ApiResponse<MonitoringPointDTO> bindBatches(
            @PathVariable String pointCode,
            @RequestBody List<String> batchNos) {
        return ApiResponse.success("批次绑定成功", monitoringPointService.bindBatches(pointCode, batchNos));
    }

    @Operation(summary = "解绑药品批次", description = "从监控点解绑指定的药品批次")
    @PostMapping("/{pointCode}/unbind-batches")
    public ApiResponse<MonitoringPointDTO> unbindBatches(
            @PathVariable String pointCode,
            @RequestBody List<String> batchNos) {
        return ApiResponse.success("批次解绑成功", monitoringPointService.unbindBatches(pointCode, batchNos));
    }

    @Operation(summary = "按批次查询关联监控点", description = "查询某个药品批次被绑定到哪些监控点")
    @GetMapping("/by-batch/{batchNo}")
    public ApiResponse<List<MonitoringPointDTO>> getMonitoringPointsByBatch(@PathVariable String batchNo) {
        return ApiResponse.success(monitoringPointService.getMonitoringPointsByBatchNo(batchNo));
    }
}
