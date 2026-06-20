package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.consultation.*;
import com.pharmacy.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/consultation")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping("/initiate")
    public ApiResponse<ConsultationDetailDTO> initiateConsultation(
            @Valid @RequestBody ConsultationInitiateDTO dto) {
        log.info("发起协同会诊，处方: {}, 发起药师: {}", dto.getPrescriptionNo(), dto.getPharmacistId());
        ConsultationDetailDTO result = consultationService.initiateConsultation(dto);
        return ApiResponse.success("会诊发起成功", result);
    }

    @PostMapping("/{consultationId}/opinion")
    public ApiResponse<ConsultationDetailDTO> submitOpinion(
            @PathVariable Long consultationId,
            @Valid @RequestBody ConsultationOpinionSubmitDTO dto) {
        log.info("提交会诊意见，会诊ID: {}, 药师: {}", consultationId, dto.getPharmacistId());
        ConsultationDetailDTO result = consultationService.submitOpinion(consultationId, dto);
        return ApiResponse.success("意见提交成功", result);
    }

    @GetMapping("/{consultationId}")
    public ApiResponse<ConsultationDetailDTO> getConsultationDetail(@PathVariable Long consultationId) {
        ConsultationDetailDTO result = consultationService.getConsultationDetail(consultationId);
        return ApiResponse.success(result);
    }

    @GetMapping("/prescription/{prescriptionNo}")
    public ApiResponse<ConsultationDetailDTO> getConsultationByPrescriptionNo(
            @PathVariable String prescriptionNo) {
        ConsultationDetailDTO result = consultationService.getConsultationByPrescriptionNo(prescriptionNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/my-todo")
    public ApiResponse<List<ConsultationTodoItemDTO>> getMyTodoList(
            @RequestParam String pharmacistId) {
        List<ConsultationTodoItemDTO> result = consultationService.getMyTodoList(pharmacistId);
        return ApiResponse.success(result);
    }

    @GetMapping("/my-history")
    public ApiResponse<List<PharmacistConsultationHistoryDTO>> getMyConsultationHistory(
            @RequestParam String pharmacistId) {
        List<PharmacistConsultationHistoryDTO> result =
                consultationService.getMyConsultationHistory(pharmacistId);
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics")
    public ApiResponse<List<ConsultationStatisticsDTO>> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ConsultationStatisticsDTO> result =
                consultationService.getStatisticsByDateRange(startDate, endDate);
        return ApiResponse.success(result);
    }
}
