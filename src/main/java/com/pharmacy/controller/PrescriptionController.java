package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.CancelPrescriptionDTO;
import com.pharmacy.dto.DispenseConfirmDTO;
import com.pharmacy.dto.PharmacistConfirmDTO;
import com.pharmacy.dto.PrescriptionDTO;
import com.pharmacy.dto.PrescriptionSubmitDTO;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.service.PrescriptionService;
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
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    public ApiResponse<PrescriptionDTO> submitPrescription(
            @Valid @RequestBody PrescriptionSubmitDTO dto) {
        log.info("收到处方提交请求: {}", dto.getPrescriptionNo());
        PrescriptionDTO result = prescriptionService.submitPrescription(dto);
        return ApiResponse.success("处方提交成功", result);
    }

    @PostMapping("/pharmacist-confirm")
    public ApiResponse<PrescriptionDTO> pharmacistConfirm(
            @Valid @RequestBody PharmacistConfirmDTO dto) {
        log.info("药师确认处方: {}", dto.getPrescriptionNo());
        PrescriptionDTO result = prescriptionService.pharmacistConfirm(dto);
        return ApiResponse.success("处理完成", result);
    }

    @PostMapping("/dispense")
    public ApiResponse<PrescriptionDTO> dispenseConfirm(
            @Valid @RequestBody DispenseConfirmDTO dto) {
        log.info("发药确认: {}", dto.getPrescriptionNo());
        PrescriptionDTO result = prescriptionService.dispenseConfirm(dto);
        return ApiResponse.success("发药完成", result);
    }

    @PostMapping("/cancel")
    public ApiResponse<PrescriptionDTO> cancelPrescription(
            @Valid @RequestBody CancelPrescriptionDTO dto) {
        log.info("取消处方: {}", dto.getPrescriptionNo());
        PrescriptionDTO result = prescriptionService.cancelPrescription(dto);
        return ApiResponse.success("处方已取消", result);
    }

    @PostMapping("/{prescriptionNo}/retry-preoccupy")
    public ApiResponse<PrescriptionDTO> retryPreoccupy(
            @PathVariable String prescriptionNo) {
        log.info("重试库存预占: {}", prescriptionNo);
        PrescriptionDTO result = prescriptionService.retryPreoccupy(prescriptionNo);
        return ApiResponse.success("预占重试完成", result);
    }

    @GetMapping("/{prescriptionNo}")
    public ApiResponse<PrescriptionDTO> getPrescription(
            @PathVariable String prescriptionNo) {
        PrescriptionDTO result = prescriptionService.getPrescription(prescriptionNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<PrescriptionDTO>> getPrescriptionsByPatient(
            @PathVariable String patientId) {
        List<PrescriptionDTO> result = prescriptionService.getPrescriptionsByPatient(patientId);
        return ApiResponse.success(result);
    }

    @GetMapping("/status/{status}")
    public ApiResponse<Page<PrescriptionDTO>> getPrescriptionsByStatus(
            @PathVariable PrescriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PrescriptionDTO> result = prescriptionService.getPrescriptionsByStatus(status, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<Page<PrescriptionDTO>> getAllPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PrescriptionDTO> result = prescriptionService.getAllPrescriptions(pageable);
        return ApiResponse.success(result);
    }
}
