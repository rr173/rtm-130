package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.reviewrule.GrayReleaseConfigDTO;
import com.pharmacy.dto.reviewrule.GrayReleaseCreateDTO;
import com.pharmacy.dto.reviewrule.GrayReleaseReportDTO;
import com.pharmacy.service.reviewrule.GrayReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gray-release")
@RequiredArgsConstructor
public class GrayReleaseController {

    private final GrayReleaseService grayReleaseService;

    @GetMapping
    public ApiResponse<List<GrayReleaseConfigDTO>> getAll() {
        return ApiResponse.success(grayReleaseService.getAllGrayReleases());
    }

    @GetMapping("/{id}")
    public ApiResponse<GrayReleaseConfigDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(grayReleaseService.getGrayRelease(id));
    }

    @PostMapping
    public ApiResponse<GrayReleaseConfigDTO> create(@Valid @RequestBody GrayReleaseCreateDTO dto) {
        log.info("创建灰度发布, 新版本: {}, 科室: {}, 创建人: {}",
                dto.getNewConfigVersion(), dto.getDepartments(), dto.getCreatedBy());
        GrayReleaseConfigDTO result = grayReleaseService.createGrayRelease(dto);
        return ApiResponse.success("灰度发布已创建并开始生效", result);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<GrayReleaseConfigDTO> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String operator = body != null ? body.get("operator") : "system";
        String reason = body != null ? body.get("reason") : null;
        log.info("取消灰度发布, id: {}, 操作人: {}", id, operator);
        GrayReleaseConfigDTO result = grayReleaseService.cancelGrayRelease(id, operator, reason);
        return ApiResponse.success("灰度发布已取消", result);
    }

    @PostMapping("/{id}/release")
    public ApiResponse<GrayReleaseConfigDTO> fullyRelease(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String operator = body != null ? body.get("operator") : "system";
        log.info("全量发布灰度配置, id: {}, 操作人: {}", id, operator);
        GrayReleaseConfigDTO result = grayReleaseService.fullyRelease(id, operator);
        return ApiResponse.success("灰度配置已全量发布，新规则已生效", result);
    }

    @GetMapping("/{id}/report")
    public ApiResponse<GrayReleaseReportDTO> getReport(@PathVariable Long id) {
        return ApiResponse.success(grayReleaseService.generateReport(id));
    }
}
