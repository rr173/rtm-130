package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.DrugReturnApplyDTO;
import com.pharmacy.dto.DrugReturnDTO;
import com.pharmacy.dto.DrugReturnItemBatchDTO;
import com.pharmacy.dto.DrugReturnReviewDTO;
import com.pharmacy.enums.DrugReturnStatus;
import com.pharmacy.service.DrugReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/drug-returns")
@RequiredArgsConstructor
public class DrugReturnController {

    private final DrugReturnService drugReturnService;

    @PostMapping("/apply")
    public ApiResponse<DrugReturnDTO> applyReturn(
            @Valid @RequestBody DrugReturnApplyDTO dto) {
        log.info("收到退药申请: 处方号[{}]", dto.getPrescriptionNo());
        DrugReturnDTO result = drugReturnService.applyReturn(dto);
        return ApiResponse.success("退药申请已提交", result);
    }

    @PostMapping("/review")
    public ApiResponse<DrugReturnDTO> reviewReturn(
            @Valid @RequestBody DrugReturnReviewDTO dto) {
        log.info("退药审核: 退药单号[{}]", dto.getReturnNo());
        DrugReturnDTO result = drugReturnService.reviewReturn(dto);
        return ApiResponse.success("审核完成", result);
    }

    @GetMapping("/{returnNo}")
    public ApiResponse<DrugReturnDTO> getReturn(@PathVariable String returnNo) {
        DrugReturnDTO result = drugReturnService.getReturn(returnNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/prescription/{prescriptionNo}")
    public ApiResponse<List<DrugReturnDTO>> getReturnsByPrescription(
            @PathVariable String prescriptionNo) {
        List<DrugReturnDTO> result = drugReturnService.getReturnsByPrescription(prescriptionNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<DrugReturnDTO>> getReturnsByPatient(
            @PathVariable String patientId) {
        List<DrugReturnDTO> result = drugReturnService.getReturnsByPatient(patientId);
        return ApiResponse.success(result);
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<DrugReturnDTO>> getReturnsByStatus(
            @PathVariable DrugReturnStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DrugReturnDTO> result = drugReturnService.getReturnsByStatus(status, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<Page<DrugReturnDTO>> getAllReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DrugReturnDTO> result = drugReturnService.getAllReturns(pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/batch/{batchNo}")
    public ApiResponse<List<DrugReturnItemBatchDTO>> getReturnBatchesByBatchNo(
            @PathVariable String batchNo) {
        List<DrugReturnItemBatchDTO> result = drugReturnService.getReturnBatchesByBatchNo(batchNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/pending")
    public ApiResponse<List<DrugReturnItemBatchDTO>> getPendingReturnBatches() {
        List<DrugReturnItemBatchDTO> result = drugReturnService.getPendingReturnBatches();
        return ApiResponse.success(result);
    }

    @GetMapping("/pending/drug/{drugCode}")
    public ApiResponse<List<DrugReturnItemBatchDTO>> getPendingReturnsByDrugCode(
            @PathVariable String drugCode) {
        List<DrugReturnItemBatchDTO> result = drugReturnService.getPendingReturnsByDrugCode(drugCode);
        return ApiResponse.success(result);
    }
}
