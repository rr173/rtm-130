package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.BatchEnvironmentHistoryDTO;
import com.pharmacy.dto.coldchain.DisposalRequestDTO;
import com.pharmacy.dto.coldchain.PendingDisposalBatchDTO;
import com.pharmacy.entity.ColdChainDisposalRecord;
import com.pharmacy.service.coldchain.ColdChainDisposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cold-chain")
@RequiredArgsConstructor
public class ColdChainDisposalController {

    private final ColdChainDisposalService coldChainDisposalService;

    @GetMapping("/pending-batches")
    public ApiResponse<List<PendingDisposalBatchDTO>> getPendingDisposalBatches() {
        return ApiResponse.success(coldChainDisposalService.getPendingDisposalBatches());
    }

    @PostMapping("/dispose")
    public ApiResponse<ColdChainDisposalRecord> disposeBatch(@Valid @RequestBody DisposalRequestDTO request) {
        return ApiResponse.success("处置完成", coldChainDisposalService.disposeBatch(request));
    }

    @GetMapping("/batch-history/{batchNo}")
    public ApiResponse<BatchEnvironmentHistoryDTO> getBatchEnvironmentHistory(@PathVariable String batchNo) {
        return ApiResponse.success(coldChainDisposalService.getBatchEnvironmentHistory(batchNo));
    }

    @GetMapping("/disposal-records/by-alert/{alertId}")
    public ApiResponse<List<ColdChainDisposalRecord>> getDisposalRecordsByAlert(@PathVariable Long alertId) {
        return ApiResponse.success(coldChainDisposalService.getDisposalRecordsByAlertId(alertId));
    }

    @GetMapping("/disposal-records/by-batch/{batchNo}")
    public ApiResponse<List<ColdChainDisposalRecord>> getDisposalRecordsByBatch(@PathVariable String batchNo) {
        return ApiResponse.success(coldChainDisposalService.getDisposalRecordsByBatchNo(batchNo));
    }
}
