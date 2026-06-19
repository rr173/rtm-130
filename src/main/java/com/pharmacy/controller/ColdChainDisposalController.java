package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.coldchain.BatchEnvironmentHistoryDTO;
import com.pharmacy.dto.coldchain.DisposalRequestDTO;
import com.pharmacy.dto.coldchain.PendingDisposalBatchDTO;
import com.pharmacy.entity.ColdChainDisposalRecord;
import com.pharmacy.service.coldchain.ColdChainDisposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "冷链中断处置", description = "红色严重告警触发的冷链中断应急处置流程")
@RestController
@RequestMapping("/cold-chain")
@RequiredArgsConstructor
public class ColdChainDisposalController {

    private final ColdChainDisposalService coldChainDisposalService;

    @Operation(summary = "查询待处置批次列表", description = "获取所有状态为冷链中断待检的药品批次，供药师进行处置")
    @GetMapping("/pending-batches")
    public ApiResponse<List<PendingDisposalBatchDTO>> getPendingDisposalBatches() {
        return ApiResponse.success(coldChainDisposalService.getPendingDisposalBatches());
    }

    @Operation(summary = "提交处置决定", description = "药师对冷链中断的批次做处置决定：复检通过 或 报损")
    @PostMapping("/dispose")
    public ApiResponse<ColdChainDisposalRecord> disposeBatch(@Valid @RequestBody DisposalRequestDTO request) {
        return ApiResponse.success("处置完成", coldChainDisposalService.disposeBatch(request));
    }

    @Operation(summary = "查询批次环境异常历史", description = "查询指定批次经历过的所有温湿度异常记录和处置情况")
    @GetMapping("/batch-history/{batchNo}")
    public ApiResponse<BatchEnvironmentHistoryDTO> getBatchEnvironmentHistory(@PathVariable String batchNo) {
        return ApiResponse.success(coldChainDisposalService.getBatchEnvironmentHistory(batchNo));
    }

    @Operation(summary = "按告警ID查询处置记录", description = "查询某个红色严重告警关联的所有批次处置记录")
    @GetMapping("/disposal-records/by-alert/{alertId}")
    public ApiResponse<List<ColdChainDisposalRecord>> getDisposalRecordsByAlert(@PathVariable Long alertId) {
        return ApiResponse.success(coldChainDisposalService.getDisposalRecordsByAlertId(alertId));
    }

    @Operation(summary = "按批次号查询处置记录", description = "查询某个批次的所有冷链处置历史记录")
    @GetMapping("/disposal-records/by-batch/{batchNo}")
    public ApiResponse<List<ColdChainDisposalRecord>> getDisposalRecordsByBatch(@PathVariable String batchNo) {
        return ApiResponse.success(coldChainDisposalService.getDisposalRecordsByBatchNo(batchNo));
    }
}
