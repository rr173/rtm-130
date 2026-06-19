package com.pharmacy.controller;

import com.pharmacy.dto.*;
import com.pharmacy.enums.ExpiryWarningLevel;
import com.pharmacy.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping("/stock-in")
    public ApiResponse<DrugBatchDTO> batchStockIn(@Valid @RequestBody BatchStockInDTO dto) {
        log.info("批次入库: 药品[{}], 批号[{}]", dto.getDrugCode(), dto.getBatchNo());
        DrugBatchDTO result = batchService.stockIn(dto);
        return ApiResponse.success("批次入库成功", result);
    }

    @GetMapping("/drug/{drugCode}")
    public ApiResponse<List<DrugBatchDTO>> getBatchesByDrug(@PathVariable String drugCode) {
        List<DrugBatchDTO> result = batchService.getBatchesByDrugCode(drugCode);
        return ApiResponse.success(result);
    }

    @GetMapping("/{batchId}")
    public ApiResponse<DrugBatchDTO> getBatchById(@PathVariable Long batchId) {
        DrugBatchDTO result = batchService.getBatchById(batchId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{batchId}/lifecycle")
    public ApiResponse<BatchLifecycleDTO> getBatchLifecycle(@PathVariable Long batchId) {
        BatchLifecycleDTO result = batchService.getBatchLifecycle(batchId);
        return ApiResponse.success(result);
    }

    @GetMapping("/warnings")
    public ApiResponse<List<ExpiryWarningDTO>> getExpiryWarnings(
            @RequestParam(required = false) ExpiryWarningLevel level,
            @RequestParam(required = false) String drugCode) {
        List<ExpiryWarningDTO> result = batchService.getExpiryWarnings(level, drugCode);
        return ApiResponse.success(result);
    }

    @PostMapping("/scan-expiry")
    public ApiResponse<String> scanExpiry() {
        batchService.scanAndUpdateExpiryStatus();
        return ApiResponse.success("效期扫描完成");
    }

    @PostMapping("/recall")
    public ApiResponse<BatchRecallResultDTO> recallBatch(@Valid @RequestBody BatchRecallDTO dto) {
        log.info("批次召回: 批号[{}]", dto.getBatchNo());
        BatchRecallResultDTO result = batchService.recallBatch(dto);
        return ApiResponse.success("批次召回完成", result);
    }

    @PostMapping("/split")
    public ApiResponse<DrugSplitRecordDTO> performSplit(@Valid @RequestBody SplitOperationDTO dto) {
        log.info("拆零操作: 批次ID[{}]", dto.getBatchId());
        DrugSplitRecordDTO result = batchService.performSplit(dto);
        return ApiResponse.success("拆零成功", result);
    }

    @PostMapping("/{batchId}/release-split-lock")
    public ApiResponse<String> releaseSplitLock(
            @PathVariable Long batchId,
            @RequestParam(required = false) String operator) {
        batchService.releaseSplitLock(batchId, operator);
        return ApiResponse.success("拆零锁已释放");
    }

    @GetMapping("/split/drug/{drugCode}")
    public ApiResponse<List<DrugSplitRecordDTO>> getSplitRecordsByDrug(@PathVariable String drugCode) {
        List<DrugSplitRecordDTO> result = batchService.getSplitRecordsByDrug(drugCode);
        return ApiResponse.success(result);
    }

    @GetMapping("/split/batch/{batchId}")
    public ApiResponse<List<DrugSplitRecordDTO>> getSplitRecordsByBatch(@PathVariable Long batchId) {
        List<DrugSplitRecordDTO> result = batchService.getSplitRecordsByBatch(batchId);
        return ApiResponse.success(result);
    }

    @GetMapping("/trace/patient/{patientId}")
    public ApiResponse<List<PatientDrugTraceDTO>> getPatientDrugTrace(@PathVariable String patientId) {
        List<PatientDrugTraceDTO> result = batchService.getPatientDrugTrace(patientId);
        return ApiResponse.success(result);
    }
}
