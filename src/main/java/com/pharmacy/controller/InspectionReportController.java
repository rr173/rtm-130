package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.InspectionReportDTO;
import com.pharmacy.service.coldchain.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inspection-reports")
@RequiredArgsConstructor
public class InspectionReportController {

    private final InspectionService inspectionService;

    @GetMapping
    public ApiResponse<List<InspectionReportDTO>> getInspectionReportList() {
        return ApiResponse.success(inspectionService.getInspectionReportList());
    }

    @GetMapping("/{reportId}")
    public ApiResponse<InspectionReportDTO> getInspectionReportDetail(@PathVariable Long reportId) {
        return ApiResponse.success(inspectionService.getInspectionReportDetail(reportId));
    }

    @PostMapping("/execute")
    public ApiResponse<InspectionReportDTO> executeInspection() {
        return ApiResponse.success("手动巡检执行完成", inspectionService.executeDailyInspection());
    }
}
