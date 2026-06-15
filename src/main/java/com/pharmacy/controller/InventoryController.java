package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.DrugDTO;
import com.pharmacy.dto.StockInDTO;
import com.pharmacy.dto.StockAdjustDTO;
import com.pharmacy.dto.InventoryLogDTO;
import com.pharmacy.entity.Drug;
import com.pharmacy.enums.InventoryLogType;
import com.pharmacy.service.InventoryService;
import com.pharmacy.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final QueryService queryService;

    @PostMapping("/stock-in")
    public ApiResponse<DrugDTO> stockIn(@Valid @RequestBody StockInDTO dto) {
        log.info("药品入库: {} 数量: {}", dto.getDrugCode(), dto.getQuantity());
        Drug drug = inventoryService.stockIn(dto);
        return ApiResponse.success("入库成功", DrugDTO.fromEntity(drug));
    }

    @PostMapping("/adjust")
    public ApiResponse<DrugDTO> adjustStock(@Valid @RequestBody StockAdjustDTO dto) {
        log.info("盘点修正: {} 修正后库存: {}", dto.getDrugCode(), dto.getActualStock());
        Drug drug = inventoryService.adjustStock(dto);
        return ApiResponse.success("盘点修正完成", DrugDTO.fromEntity(drug));
    }

    @GetMapping("/drugs")
    public ApiResponse<List<DrugDTO>> getAllDrugInventories() {
        List<DrugDTO> result = queryService.getAllDrugInventories();
        return ApiResponse.success(result);
    }

    @GetMapping("/drugs/{drugCode}")
    public ApiResponse<DrugDTO> getDrugInventory(@PathVariable String drugCode) {
        DrugDTO result = queryService.getDrugInventory(drugCode);
        return ApiResponse.success(result);
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getInventorySummary() {
        Map<String, Object> result = queryService.getInventorySummary();
        return ApiResponse.success(result);
    }

    @GetMapping("/logs")
    public ApiResponse<List<InventoryLogDTO>> getInventoryLogs(
            @RequestParam(required = false) String drugCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<InventoryLogDTO> result = queryService.getInventoryLogs(drugCode, startDate, endDate);
        return ApiResponse.success(result);
    }

    @GetMapping("/logs/type/{type}")
    public ApiResponse<List<InventoryLogDTO>> getInventoryLogsByType(
            @PathVariable InventoryLogType type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<InventoryLogDTO> result = queryService.getInventoryLogsByType(type, startDate, endDate);
        return ApiResponse.success(result);
    }

    @GetMapping("/logs/prescription/{prescriptionNo}")
    public ApiResponse<List<InventoryLogDTO>> getInventoryLogsByPrescription(
            @PathVariable String prescriptionNo) {
        List<InventoryLogDTO> result = queryService.getInventoryLogsByPrescription(prescriptionNo);
        return ApiResponse.success(result);
    }
}
