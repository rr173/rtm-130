package com.pharmacy.controller;

import com.pharmacy.dto.*;
import com.pharmacy.service.DispenseQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/dispense-window")
@RequiredArgsConstructor
public class DispenseWindowController {

    private final DispenseQueueService dispenseQueueService;

    @PostMapping("/queue/enqueue/{prescriptionNo}")
    public ApiResponse<QueueItemDTO> enqueue(@PathVariable String prescriptionNo) {
        log.info("处方加入配药排队: {}", prescriptionNo);
        QueueItemDTO result = dispenseQueueService.enqueue(prescriptionNo);
        return ApiResponse.success("处方已加入排队队列", result);
    }

    @PostMapping("/window/claim")
    public ApiResponse<QueueItemDTO> claimNext(@Valid @RequestBody WindowClaimDTO dto) {
        log.info("窗口[{}]领取下一张处方", dto.getWindowNo());
        QueueItemDTO result = dispenseQueueService.claimNext(
                dto.getWindowNo(), dto.getPharmacistId(), dto.getPharmacistName());
        return ApiResponse.success("领取成功", result);
    }

    @PostMapping("/window/complete")
    public ApiResponse<WindowDTO> completeDispense(@Valid @RequestBody WindowCompleteDTO dto) {
        log.info("窗口[{}]确认配药完成", dto.getWindowNo());
        WindowDTO result = dispenseQueueService.completeDispense(dto.getWindowNo());
        return ApiResponse.success("配药完成，处方已流转到发药确认环节", result);
    }

    @PostMapping("/window/return")
    public ApiResponse<WindowDTO> returnPrescription(@Valid @RequestBody WindowCompleteDTO dto) {
        log.info("窗口[{}]释放处方", dto.getWindowNo());
        WindowDTO result = dispenseQueueService.returnPrescription(dto.getWindowNo());
        return ApiResponse.success("处方已退回队列", result);
    }

    @PostMapping("/window/{windowNo}/close")
    public ApiResponse<WindowDTO> closeWindow(@PathVariable String windowNo) {
        log.info("关闭窗口[{}]", windowNo);
        WindowDTO result = dispenseQueueService.closeWindow(windowNo);
        return ApiResponse.success("窗口已关闭", result);
    }

    @PostMapping("/window/{windowNo}/open")
    public ApiResponse<WindowDTO> openWindow(@PathVariable String windowNo) {
        log.info("开启窗口[{}]", windowNo);
        WindowDTO result = dispenseQueueService.openWindow(windowNo);
        return ApiResponse.success("窗口已开启", result);
    }

    @GetMapping("/windows")
    public ApiResponse<List<WindowDTO>> getAllWindows() {
        List<WindowDTO> result = dispenseQueueService.getAllWindows();
        return ApiResponse.success(result);
    }

    @GetMapping("/window/{windowNo}")
    public ApiResponse<WindowDTO> getWindow(@PathVariable String windowNo) {
        WindowDTO result = dispenseQueueService.getWindow(windowNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/queue")
    public ApiResponse<List<QueueItemDTO>> getQueueList() {
        List<QueueItemDTO> result = dispenseQueueService.getQueueList();
        return ApiResponse.success(result);
    }

    @GetMapping("/queue/position/{prescriptionNo}")
    public ApiResponse<QueuePositionDTO> getQueuePosition(@PathVariable String prescriptionNo) {
        QueuePositionDTO result = dispenseQueueService.getQueuePosition(prescriptionNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics/window")
    public ApiResponse<List<WindowStatisticsDTO>> getWindowStatistics(
            @RequestParam(required = false) LocalDate date) {
        List<WindowStatisticsDTO> result = dispenseQueueService.getWindowStatistics(date);
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics/queue")
    public ApiResponse<List<QueueStatisticsDTO>> getQueueStatistics(
            @RequestParam(required = false) LocalDate date) {
        List<QueueStatisticsDTO> result = dispenseQueueService.getQueueStatistics(date);
        return ApiResponse.success(result);
    }
}
