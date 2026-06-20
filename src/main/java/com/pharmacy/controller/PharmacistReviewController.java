package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.pharmacistreview.ClaimPrescriptionDTO;
import com.pharmacy.dto.pharmacistreview.PharmacistReviewDetailDTO;
import com.pharmacy.dto.pharmacistreview.PharmacistReviewSubmitDTO;
import com.pharmacy.dto.pharmacistreview.ReviewTodoItemDTO;
import com.pharmacy.service.PharmacistReviewService;
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
@RequestMapping("/pharmacist-review")
@RequiredArgsConstructor
public class PharmacistReviewController {

    private final PharmacistReviewService pharmacistReviewService;

    @GetMapping("/todo-pool")
    public ApiResponse<Page<ReviewTodoItemDTO>> getTodoPool(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewTodoItemDTO> result = pharmacistReviewService.getTodoPool(pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/todo-pool/all")
    public ApiResponse<List<ReviewTodoItemDTO>> getAllTodoPool() {
        List<ReviewTodoItemDTO> result = pharmacistReviewService.getTodoPool();
        return ApiResponse.success(result);
    }

    @GetMapping("/my-in-review")
    public ApiResponse<List<ReviewTodoItemDTO>> getMyInReviewPrescriptions(
            @RequestParam String pharmacistId) {
        List<ReviewTodoItemDTO> result = pharmacistReviewService.getMyInReviewPrescriptions(pharmacistId);
        return ApiResponse.success(result);
    }

    @PostMapping("/claim/{prescriptionNo}")
    public ApiResponse<ReviewTodoItemDTO> claimPrescription(
            @PathVariable String prescriptionNo,
            @Valid @RequestBody ClaimPrescriptionDTO dto) {
        log.info("药师[{}]领取处方[{}]", dto.getPharmacistId(), prescriptionNo);
        ReviewTodoItemDTO result = pharmacistReviewService.claimPrescription(
                prescriptionNo, dto.getPharmacistId(), dto.getPharmacistName());
        return ApiResponse.success("领取成功", result);
    }

    @PostMapping("/claim-next")
    public ApiResponse<ReviewTodoItemDTO> claimNextPrescription(
            @Valid @RequestBody ClaimPrescriptionDTO dto) {
        log.info("药师[{}]领取下一张处方", dto.getPharmacistId());
        ReviewTodoItemDTO result = pharmacistReviewService.claimNextPrescription(
                dto.getPharmacistId(), dto.getPharmacistName());
        return ApiResponse.success("领取成功", result);
    }

    @PostMapping("/submit")
    public ApiResponse<PharmacistReviewDetailDTO> submitReview(
            @Valid @RequestBody PharmacistReviewSubmitDTO dto) {
        log.info("药师[{}]提交处方[{}]审核结果", dto.getPharmacistId(), dto.getPrescriptionNo());
        PharmacistReviewDetailDTO result = pharmacistReviewService.submitReview(dto);
        return ApiResponse.success("审核完成", result);
    }

    @PostMapping("/release/{prescriptionNo}")
    public ApiResponse<ReviewTodoItemDTO> releasePrescription(
            @PathVariable String prescriptionNo,
            @RequestParam String pharmacistId) {
        log.info("药师[{}]释放处方[{}]", pharmacistId, prescriptionNo);
        ReviewTodoItemDTO result = pharmacistReviewService.releasePrescription(prescriptionNo, pharmacistId);
        return ApiResponse.success("已释放回待办池", result);
    }

    @GetMapping("/detail/{prescriptionNo}")
    public ApiResponse<PharmacistReviewDetailDTO> getReviewDetail(
            @PathVariable String prescriptionNo) {
        PharmacistReviewDetailDTO result = pharmacistReviewService.getReviewDetail(prescriptionNo);
        return ApiResponse.success(result);
    }
}
