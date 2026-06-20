package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.reviewrule.ReviewRuleChangeLogDTO;
import com.pharmacy.dto.reviewrule.ReviewRuleConfigDTO;
import com.pharmacy.dto.reviewrule.ReviewRuleConfigUpdateDTO;
import com.pharmacy.service.reviewrule.ReviewRuleConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/review-rule-config")
@RequiredArgsConstructor
public class ReviewRuleConfigController {

    private final ReviewRuleConfigService configService;

    @GetMapping("/current")
    public ApiResponse<ReviewRuleConfigDTO> getCurrentConfig() {
        return ApiResponse.success(configService.getCurrentConfig());
    }

    @GetMapping("/versions")
    public ApiResponse<List<ReviewRuleConfigDTO>> getAllVersions() {
        return ApiResponse.success(configService.getAllConfigs());
    }

    @GetMapping("/versions/{version}")
    public ApiResponse<ReviewRuleConfigDTO> getConfigByVersion(@PathVariable Integer version) {
        return ApiResponse.success(configService.getConfigByVersion(version));
    }

    @PutMapping("/update")
    public ApiResponse<ReviewRuleConfigDTO> updateConfig(@Valid @RequestBody ReviewRuleConfigUpdateDTO dto) {
        log.info("收到审核规则配置更新请求, 操作人: {}", dto.getOperator());
        ReviewRuleConfigDTO result = configService.updateConfig(dto);
        return ApiResponse.success("配置更新成功，已即时生效", result);
    }

    @PostMapping("/rollback/{targetVersion}")
    public ApiResponse<ReviewRuleConfigDTO> rollback(
            @PathVariable Integer targetVersion,
            @RequestBody(required = false) Map<String, String> body) {
        String operator = body != null ? body.get("operator") : "system";
        String remark = body != null ? body.get("remark") : null;
        log.info("收到配置回滚请求, 目标版本: {}, 操作人: {}", targetVersion, operator);
        ReviewRuleConfigDTO result = configService.rollbackToVersion(targetVersion, operator, remark);
        return ApiResponse.success("已回滚至版本 " + targetVersion + "，配置即时生效", result);
    }

    @GetMapping("/change-logs")
    public ApiResponse<List<ReviewRuleChangeLogDTO>> getChangeLogs(
            @RequestParam(required = false) Integer version) {
        return ApiResponse.success(configService.getChangeLogs(version));
    }
}
