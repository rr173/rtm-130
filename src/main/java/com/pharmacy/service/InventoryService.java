package com.pharmacy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.dto.StockInDTO;
import com.pharmacy.dto.StockAdjustDTO;
import com.pharmacy.entity.*;
import com.pharmacy.enums.InventoryLogType;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.InvalidStatusException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.DrugRepository;
import com.pharmacy.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final DrugRepository drugRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final BatchService batchService;
    private final ObjectMapper objectMapper;

    @Transactional
    public boolean preoccupyStock(Prescription prescription) {
        log.info("开始为处方[{}]预占库存", prescription.getPrescriptionNo());

        List<String> drugCodes = prescription.getItems().stream()
                .map(PrescriptionItem::getDrugCode)
                .distinct()
                .sorted()
                .toList();

        List<Drug> drugs = drugRepository.findByDrugCodesWithLock(drugCodes);

        Map<String, Drug> drugMap = drugs.stream()
                .collect(Collectors.toMap(Drug::getDrugCode, d -> d));

        List<String> lackDrugs = new ArrayList<>();
        Map<String, Integer> preoccupyQuantities = new HashMap<>();
        Map<String, List<BatchService.BatchAllocation>> batchAllocationsMap = new HashMap<>();

        for (PrescriptionItem item : prescription.getItems()) {
            Drug drug = drugMap.get(item.getDrugCode());
            if (drug == null) {
                throw new ResourceNotFoundException("药品不存在: " + item.getDrugCode());
            }

            int required = item.getTotalQuantity();
            int available = drug.getAvailableStock();

            if (available < required) {
                lackDrugs.add(String.format("%s(可用:%d, 需要:%d)",
                        drug.getName(), available, required));
            } else {
                preoccupyQuantities.merge(item.getDrugCode(), required, Integer::sum);
            }
        }

        if (!lackDrugs.isEmpty()) {
            String lackDetails = String.join(", ", lackDrugs);
            prescription.setLackDrugDetails(lackDetails);
            log.warn("处方[{}]预占库存失败，缺药: {}", prescription.getPrescriptionNo(), lackDetails);
            return false;
        }

        for (Map.Entry<String, Integer> entry : preoccupyQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int quantity = entry.getValue();
            Drug drug = drugMap.get(drugCode);

            List<BatchService.BatchAllocation> allocations =
                    batchService.allocateBatchesForPreoccupy(drugCode, quantity, prescription.getPrescriptionNo());

            int allocatedTotal = allocations.stream().mapToInt(a -> a.quantity).sum();
            if (allocatedTotal < quantity) {
                prescription.setLackDrugDetails(
                        String.format("%s(批次库存不足，可分配:%d, 需要:%d)",
                                drug.getName(), allocatedTotal, quantity));
                log.warn("处方[{}]预占失败，药品[{}]批次库存不足", prescription.getPrescriptionNo(), drugCode);
                return false;
            }
            batchAllocationsMap.put(drugCode, allocations);
        }

        for (Map.Entry<String, Integer> entry : preoccupyQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int quantity = entry.getValue();
            Drug drug = drugMap.get(drugCode);
            List<BatchService.BatchAllocation> allocations = batchAllocationsMap.get(drugCode);

            int beforeAvailable = drug.getAvailableStock();
            int beforePreoccupied = drug.getPreoccupiedStock();

            drug.setAvailableStock(beforeAvailable - quantity);
            drug.setPreoccupiedStock(beforePreoccupied + quantity);

            drugRepository.save(drug);

            String batchDetails = buildBatchDetailsJson(allocations);

            createInventoryLog(drug, InventoryLogType.PREOCCUPY, quantity,
                    beforeAvailable, beforeAvailable - quantity,
                    beforePreoccupied, beforePreoccupied + quantity,
                    prescription.getPrescriptionNo(), "处方预占", null,
                    null, batchDetails);

            batchService.applyPreoccupyAllocations(drugCode, prescription.getPrescriptionNo(), allocations, null);
        }

        for (PrescriptionItem item : prescription.getItems()) {
            List<BatchService.BatchAllocation> allocs = batchAllocationsMap.get(item.getDrugCode());
            if (allocs != null) {
                int remainingForItem = item.getTotalQuantity();
                for (BatchService.BatchAllocation alloc : allocs) {
                    if (remainingForItem <= 0) break;
                    int take = Math.min(alloc.quantity, remainingForItem);
                    PrescriptionItemBatch pib = new PrescriptionItemBatch();
                    pib.setBatchId(alloc.batchId);
                    pib.setDrugCode(item.getDrugCode());
                    pib.setBatchNo(alloc.batchNo);
                    pib.setQuantity(take);
                    pib.setFromSplit(alloc.fromSplit);
                    pib.setSplitRecordId(alloc.splitRecordId);
                    item.addBatchAllocation(pib);
                    remainingForItem -= take;
                }
            }
            item.setPreoccupied(true);
        }

        log.info("处方[{}]库存预占成功", prescription.getPrescriptionNo());
        return true;
    }

    @Transactional
    public void releasePreoccupyStock(Prescription prescription, String reason, String operator) {
        log.info("释放处方[{}]的预占库存，原因: {}", prescription.getPrescriptionNo(), reason);

        List<String> drugCodes = prescription.getItems().stream()
                .filter(PrescriptionItem::getPreoccupied)
                .map(PrescriptionItem::getDrugCode)
                .distinct()
                .sorted()
                .toList();

        if (drugCodes.isEmpty()) {
            log.info("处方[{}]没有需要释放的预占库存", prescription.getPrescriptionNo());
            return;
        }

        List<Drug> drugs = drugRepository.findByDrugCodesWithLock(drugCodes);
        Map<String, Drug> drugMap = drugs.stream()
                .collect(Collectors.toMap(Drug::getDrugCode, d -> d));

        Map<String, Integer> releaseQuantities = new HashMap<>();
        for (PrescriptionItem item : prescription.getItems()) {
            if (Boolean.TRUE.equals(item.getPreoccupied())) {
                releaseQuantities.merge(item.getDrugCode(), item.getTotalQuantity(), Integer::sum);
                item.setPreoccupied(false);
                item.getBatchAllocations().clear();
            }
        }

        for (Map.Entry<String, Integer> entry : releaseQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int quantity = entry.getValue();
            Drug drug = drugMap.get(drugCode);

            if (drug == null) {
                log.warn("释放预占时药品不存在: {}", drugCode);
                continue;
            }

            int beforeAvailable = drug.getAvailableStock();
            int beforePreoccupied = drug.getPreoccupiedStock();

            drug.setAvailableStock(beforeAvailable + quantity);
            drug.setPreoccupiedStock(Math.max(0, beforePreoccupied - quantity));

            drugRepository.save(drug);

            createInventoryLog(drug, InventoryLogType.RELEASE, quantity,
                    beforeAvailable, beforeAvailable + quantity,
                    beforePreoccupied, Math.max(0, beforePreoccupied - quantity),
                    prescription.getPrescriptionNo(), reason, operator,
                    null, null);

            batchService.releasePreoccupyAllocations(drugCode, prescription.getPrescriptionNo(), reason, operator);
        }

        log.info("处方[{}]预占库存释放完成", prescription.getPrescriptionNo());
    }

    @Transactional
    public void dispenseStock(Prescription prescription, String dispensedBy) {
        log.info("为处方[{}]执行发药扣减库存", prescription.getPrescriptionNo());

        for (PrescriptionItem item : prescription.getItems()) {
            if (!Boolean.TRUE.equals(item.getPreoccupied())) {
                throw new InvalidStatusException(
                        String.format("药品[%s]未预占，无法发药", item.getDrugName()));
            }
        }

        Map<String, Integer> dispenseQuantities = new HashMap<>();
        for (PrescriptionItem item : prescription.getItems()) {
            dispenseQuantities.merge(item.getDrugCode(), item.getTotalQuantity(), Integer::sum);
        }

        List<String> drugCodes = dispenseQuantities.keySet().stream()
                .sorted()
                .toList();

        List<Drug> drugs = drugRepository.findByDrugCodesWithLock(drugCodes);
        Map<String, Drug> drugMap = drugs.stream()
                .collect(Collectors.toMap(Drug::getDrugCode, d -> d));

        for (Map.Entry<String, Integer> entry : dispenseQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int totalQuantity = entry.getValue();

            Drug drug = drugMap.get(drugCode);
            if (drug == null) {
                throw new ResourceNotFoundException("药品不存在: " + drugCode);
            }

            if (drug.getPreoccupiedStock() < totalQuantity) {
                throw new BusinessException(409,
                        String.format("发药时预占数据不一致: 药品[%s]预占库存(%d)小于处方总数量(%d)，" +
                                        "可能已被盘点修正，请先取消处方后重新提交",
                                drug.getName(), drug.getPreoccupiedStock(), totalQuantity));
            }

            int actualStock = drug.getAvailableStock() + drug.getPreoccupiedStock();
            if (actualStock < totalQuantity) {
                throw new BusinessException(409,
                        String.format("发药时实际库存不足: 药品[%s]实际库存(%d)小于处方总数量(%d)，" +
                                        "请先取消处方后重新提交",
                                drug.getName(), actualStock, totalQuantity));
            }
        }

        for (Map.Entry<String, Integer> entry : dispenseQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int quantity = entry.getValue();
            Drug drug = drugMap.get(drugCode);

            int beforeAvailable = drug.getAvailableStock();
            int beforePreoccupied = drug.getPreoccupiedStock();

            drug.setPreoccupiedStock(beforePreoccupied - quantity);

            drugRepository.save(drug);

            String batchDetails = buildBatchDetailsFromPrescription(prescription, drugCode);

            createInventoryLog(drug, InventoryLogType.DISPENSE, quantity,
                    beforeAvailable, beforeAvailable,
                    beforePreoccupied, beforePreoccupied - quantity,
                    prescription.getPrescriptionNo(), "发药扣减", dispensedBy,
                    null, batchDetails);

            batchService.applyDispenseAllocations(drugCode, prescription.getPrescriptionNo(), dispensedBy);
        }

        for (PrescriptionItem item : prescription.getItems()) {
            item.setDispensed(true);
            item.setPreoccupied(false);
        }

        log.info("处方[{}]发药扣减完成", prescription.getPrescriptionNo());
    }

    @Transactional
    public Drug stockIn(StockInDTO dto) {
        log.info("药品[{}]入库，数量: {}", dto.getDrugCode(), dto.getQuantity());

        Drug drug = drugRepository.findByDrugCodeWithLock(dto.getDrugCode())
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + dto.getDrugCode()));

        int beforeAvailable = drug.getAvailableStock();
        int beforePreoccupied = drug.getPreoccupiedStock();

        int addQuantity = dto.getQuantity();
        if (Boolean.TRUE.equals(drug.getSplittable()) && drug.getPackageQuantity() != null) {
            addQuantity = dto.getQuantity() * drug.getPackageQuantity();
            log.info("可拆零药品入库: 包装数量[{}] × 每包[{}] = 最小单位[{}]",
                    dto.getQuantity(), drug.getPackageQuantity(), addQuantity);
        }

        drug.setAvailableStock(beforeAvailable + addQuantity);
        drugRepository.save(drug);

        createInventoryLog(drug, InventoryLogType.STOCK_IN, addQuantity,
                beforeAvailable, beforeAvailable + addQuantity,
                beforePreoccupied, beforePreoccupied,
                null, dto.getRemark() + (addQuantity != dto.getQuantity() ? " (包装数量:" + dto.getQuantity() + ")" : ""), dto.getOperator(),
                null, null);

        log.info("药品[{}]入库完成，入库前: {}, 入库后: {}",
                drug.getName(), beforeAvailable, drug.getAvailableStock());

        return drug;
    }

    @Transactional
    public Drug adjustStock(StockAdjustDTO dto) {
        log.info("盘点修正药品[{}]库存，修正后实际库存: {}", dto.getDrugCode(), dto.getActualStock());

        if (dto.getActualStock() < 0) {
            throw new BusinessException("库存不能为负数");
        }

        Drug drug = drugRepository.findByDrugCodeWithLock(dto.getDrugCode())
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + dto.getDrugCode()));

        int currentActual = drug.getActualStock();
        int adjustQuantity = dto.getActualStock() - currentActual;

        if (adjustQuantity == 0) {
            log.info("药品[{}]实际库存无变化，无需修正", drug.getName());
            return drug;
        }

        int beforeAvailable = drug.getAvailableStock();
        int beforePreoccupied = drug.getPreoccupiedStock();

        int newAvailable = beforeAvailable + adjustQuantity;
        if (newAvailable < 0) {
            newAvailable = 0;
            drug.setAvailableStock(0);
            drug.setPreoccupiedStock(dto.getActualStock());
        } else {
            drug.setAvailableStock(newAvailable);
        }

        drugRepository.save(drug);

        String remark = String.format("盘点修正，原实际库存: %d, 修正后: %d, 差额: %+d. %s",
                currentActual, dto.getActualStock(), adjustQuantity,
                dto.getRemark() != null ? dto.getRemark() : "");

        createInventoryLog(drug, InventoryLogType.ADJUST, adjustQuantity,
                beforeAvailable, drug.getAvailableStock(),
                beforePreoccupied, drug.getPreoccupiedStock(),
                null, remark, dto.getOperator(),
                null, null);

        log.info("药品[{}]盘点修正完成", drug.getName());

        return drug;
    }

    private void createInventoryLog(Drug drug, InventoryLogType type, int quantity,
                                    int beforeAvailable, int afterAvailable,
                                    int beforePreoccupied, int afterPreoccupied,
                                    String prescriptionNo, String remark, String operator,
                                    String batchNo, String batchDetails) {
        InventoryLog log = new InventoryLog();
        log.setDrugCode(drug.getDrugCode());
        log.setDrugName(drug.getName());
        log.setLogType(type);
        log.setQuantity(quantity);
        log.setBeforeAvailableStock(beforeAvailable);
        log.setAfterAvailableStock(afterAvailable);
        log.setBeforePreoccupiedStock(beforePreoccupied);
        log.setAfterPreoccupiedStock(afterPreoccupied);
        log.setPrescriptionNo(prescriptionNo);
        log.setRemark(remark);
        log.setOperator(operator);
        log.setBatchNo(batchNo);
        log.setBatchDetails(batchDetails);
        inventoryLogRepository.save(log);
    }

    private String buildBatchDetailsJson(List<BatchService.BatchAllocation> allocations) {
        try {
            List<Map<String, Object>> details = allocations.stream().map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("batchId", a.batchId);
                m.put("batchNo", a.batchNo);
                m.put("quantity", a.quantity);
                m.put("fromSplit", a.fromSplit);
                return m;
            }).toList();
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.error("序列化批次明细失败", e);
            return null;
        }
    }

    private String buildBatchDetailsFromPrescription(Prescription prescription, String drugCode) {
        List<Map<String, Object>> allDetails = new ArrayList<>();
        for (PrescriptionItem item : prescription.getItems()) {
            if (!item.getDrugCode().equals(drugCode)) continue;
            for (PrescriptionItemBatch pib : item.getBatchAllocations()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("batchId", pib.getBatchId());
                m.put("batchNo", pib.getBatchNo());
                m.put("quantity", pib.getQuantity());
                m.put("fromSplit", pib.getFromSplit());
                allDetails.add(m);
            }
        }
        try {
            return objectMapper.writeValueAsString(allDetails);
        } catch (JsonProcessingException e) {
            log.error("序列化批次明细失败", e);
            return null;
        }
    }

    public Drug getDrug(String drugCode) {
        return drugRepository.findByDrugCode(drugCode)
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + drugCode));
    }

    public List<Drug> getAllDrugs() {
        return drugRepository.findAll();
    }
}
