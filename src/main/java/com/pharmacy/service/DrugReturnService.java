package com.pharmacy.service;

import com.pharmacy.dto.*;
import com.pharmacy.entity.*;
import com.pharmacy.enums.*;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.InvalidStatusException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugReturnService {

    private static final List<BatchStatus> UNAVAILABLE_BATCH_STATUSES = List.of(
            BatchStatus.LOCKED, BatchStatus.RECALLED, BatchStatus.EXPIRED);

    private final DrugReturnRepository drugReturnRepository;
    private final DrugReturnItemBatchRepository drugReturnItemBatchRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PrescriptionItemBatchRepository prescriptionItemBatchRepository;
    private final DrugRepository drugRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final BatchInventoryLogRepository batchInventoryLogRepository;

    @Transactional
    public DrugReturnDTO applyReturn(DrugReturnApplyDTO dto) {
        log.info("收到退药申请: 处方号[{}], 原因[{}]", dto.getPrescriptionNo(), dto.getReturnReason());

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(dto.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + dto.getPrescriptionNo()));

        if (prescription.getStatus() != PrescriptionStatus.DISPENSED) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许退药，只有已发药处方才能退药",
                            prescription.getStatus().getDescription()));
        }

        Map<Long, PrescriptionItem> itemMap = new HashMap<>();
        for (PrescriptionItem item : prescription.getItems()) {
            itemMap.put(item.getId(), item);
        }

        for (DrugReturnItemApplyDTO itemDTO : dto.getItems()) {
            PrescriptionItem item = itemMap.get(itemDTO.getPrescriptionItemId());
            if (item == null) {
                throw new ResourceNotFoundException(
                        "处方明细不存在: " + itemDTO.getPrescriptionItemId());
            }
            if (!Boolean.TRUE.equals(item.getDispensed())) {
                throw new InvalidStatusException(
                        String.format("药品[%s]未发药，无法退药", item.getDrugName()));
            }

            int alreadyReturned = getAlreadyReturnedQuantity(
                    dto.getPrescriptionNo(), itemDTO.getPrescriptionItemId());
            int availableToReturn = item.getTotalQuantity() - alreadyReturned;

            if (itemDTO.getReturnQuantity() > availableToReturn) {
                throw new BusinessException(
                        String.format("药品[%s]退药数量超过可退数量，已发药:%d, 已退:%d, 可退:%d, 申请退:%d",
                                item.getDrugName(), item.getTotalQuantity(),
                                alreadyReturned, availableToReturn, itemDTO.getReturnQuantity()));
            }
        }

        String returnNo = generateReturnNo();
        DrugReturn drugReturn = new DrugReturn();
        drugReturn.setReturnNo(returnNo);
        drugReturn.setPrescriptionNo(dto.getPrescriptionNo());
        drugReturn.setPatientId(prescription.getPatientId());
        drugReturn.setPatientName(prescription.getPatientName());
        drugReturn.setStatus(DrugReturnStatus.PENDING_REVIEW);
        drugReturn.setReturnReason(dto.getReturnReason());
        drugReturn.setAppliedBy(dto.getAppliedBy());
        drugReturn.setAppliedAt(LocalDateTime.now());

        for (DrugReturnItemApplyDTO itemDTO : dto.getItems()) {
            PrescriptionItem prescriptionItem = itemMap.get(itemDTO.getPrescriptionItemId());

            DrugReturnItem returnItem = new DrugReturnItem();
            returnItem.setDrugCode(prescriptionItem.getDrugCode());
            returnItem.setDrugName(prescriptionItem.getDrugName());
            returnItem.setPrescriptionItemId(prescriptionItem.getId());
            returnItem.setReturnQuantity(itemDTO.getReturnQuantity());
            returnItem.setOriginalDispensedQuantity(prescriptionItem.getTotalQuantity());
            returnItem.setReturnItemReason(itemDTO.getReturnItemReason());

            allocateReturnToBatches(prescriptionItem, itemDTO.getReturnQuantity(), returnItem);

            drugReturn.addItem(returnItem);
        }

        drugReturn = drugReturnRepository.save(drugReturn);
        log.info("退药申请已提交: 退药单号[{}]", returnNo);

        return DrugReturnDTO.fromEntity(drugReturn);
    }

    private int getAlreadyReturnedQuantity(String prescriptionNo, Long prescriptionItemId) {
        Integer sum = drugReturnRepository.sumReturnedQuantityByPrescriptionItem(
                prescriptionNo, prescriptionItemId);
        return sum != null ? sum : 0;
    }

    private void allocateReturnToBatches(PrescriptionItem prescriptionItem,
                                         int returnQuantity, DrugReturnItem returnItem) {
        List<PrescriptionItemBatch> originalBatches = prescriptionItem.getBatchAllocations();
        if (originalBatches == null || originalBatches.isEmpty()) {
            throw new BusinessException(
                    "处方明细没有批次分配记录，无法退药: " + prescriptionItem.getDrugName());
        }

        int remaining = returnQuantity;
        for (PrescriptionItemBatch pib : originalBatches) {
            if (remaining <= 0) break;

            int batchReturnQty = Math.min(pib.getQuantity(), remaining);

            DrugReturnItemBatch returnBatch = new DrugReturnItemBatch();
            returnBatch.setBatchId(pib.getBatchId());
            returnBatch.setDrugCode(pib.getDrugCode());
            returnBatch.setBatchNo(pib.getBatchNo());
            returnBatch.setReturnQuantity(batchReturnQty);
            returnBatch.setOriginalDispensedQuantity(pib.getQuantity());
            returnBatch.setFromSplit(pib.getFromSplit());
            returnBatch.setPending(false);

            returnItem.addBatchAllocation(returnBatch);
            remaining -= batchReturnQty;
        }

        if (remaining > 0) {
            throw new BusinessException("批次分配计算错误，退药数量不匹配");
        }
    }

    @Transactional
    public DrugReturnDTO reviewReturn(DrugReturnReviewDTO dto) {
        log.info("退药审核: 退药单号[{}], 结果[{}]", dto.getReturnNo(),
                dto.getApproved() ? "通过" : "拒绝");

        DrugReturn drugReturn = drugReturnRepository.findByReturnNoWithLock(dto.getReturnNo())
                .orElseThrow(() -> new ResourceNotFoundException("退药单不存在: " + dto.getReturnNo()));

        if (drugReturn.getStatus() != DrugReturnStatus.PENDING_REVIEW) {
            throw new InvalidStatusException(
                    String.format("退药单状态[%s]不允许审核操作",
                            drugReturn.getStatus().getDescription()));
        }

        drugReturn.setReviewedBy(dto.getReviewedBy());
        drugReturn.setReviewedAt(LocalDateTime.now());
        drugReturn.setReviewComment(dto.getReviewComment());

        if (Boolean.TRUE.equals(dto.getApproved())) {
            approveReturn(drugReturn);
        } else {
            rejectReturn(drugReturn, dto.getLossReason());
        }

        drugReturn = drugReturnRepository.save(drugReturn);
        log.info("退药审核完成: 退药单号[{}], 状态[{}]",
                drugReturn.getReturnNo(), drugReturn.getStatus());

        return DrugReturnDTO.fromEntity(drugReturn);
    }

    private void approveReturn(DrugReturn drugReturn) {
        drugReturn.setStatus(DrugReturnStatus.APPROVED);

        Map<String, Integer> drugReturnQuantities = new HashMap<>();
        Map<String, Integer> drugActualIncreaseMap = new HashMap<>();

        for (DrugReturnItem item : drugReturn.getItems()) {
            drugReturnQuantities.merge(item.getDrugCode(), item.getReturnQuantity(), Integer::sum);

            for (DrugReturnItemBatch returnBatch : item.getBatchAllocations()) {
                processReturnBatch(drugReturn, item, returnBatch);
                if (!Boolean.TRUE.equals(returnBatch.getPending())) {
                    drugActualIncreaseMap.merge(item.getDrugCode(),
                            returnBatch.getReturnQuantity(), Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : drugReturnQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int totalQty = entry.getValue();
            int actualStockIncrease = drugActualIncreaseMap.getOrDefault(drugCode, 0);

            Drug drug = drugRepository.findByDrugCodeWithLock(drugCode).orElse(null);
            if (drug == null) {
                log.warn("退药时药品不存在: {}", drugCode);
                continue;
            }

            int beforeAvailable = drug.getAvailableStock();
            int beforePreoccupied = drug.getPreoccupiedStock();

            drug.setAvailableStock(beforeAvailable + actualStockIncrease);
            drugRepository.save(drug);

            int pendingQty = totalQty - actualStockIncrease;
            String remark = "退药回库" + (pendingQty > 0 ? " (待处理:" + pendingQty + ")" : "");

            createInventoryLog(drug, InventoryLogType.RETURN_STOCK, totalQty,
                    beforeAvailable, drug.getAvailableStock(),
                    beforePreoccupied, beforePreoccupied,
                    drugReturn.getPrescriptionNo(), remark,
                    drugReturn.getReviewedBy(),
                    null, null);

            if (pendingQty > 0) {
                createInventoryLog(drug, InventoryLogType.RETURN_PENDING, pendingQty,
                        beforeAvailable, beforeAvailable,
                        beforePreoccupied, beforePreoccupied,
                        drugReturn.getPrescriptionNo(), "退药进入待处理池",
                        drugReturn.getReviewedBy(),
                        null, null);
            }
        }
    }

    private void processReturnBatch(DrugReturn drugReturn, DrugReturnItem item,
                                    DrugReturnItemBatch returnBatch) {
        DrugBatch batch = drugBatchRepository.findByIdWithLock(returnBatch.getBatchId()).orElse(null);

        if (batch == null) {
            returnBatch.setPending(true);
            returnBatch.setPendingReason("批次不存在");
            log.warn("退药时批次不存在: batchId={}", returnBatch.getBatchId());
            return;
        }

        if (UNAVAILABLE_BATCH_STATUSES.contains(batch.getStatus())) {
            returnBatch.setPending(true);
            returnBatch.setPendingReason("批次状态不可用: " + batch.getStatus().getDescription());
            log.info("退药批次[{}]状态为[{}]，进入待处理池",
                    batch.getBatchNo(), batch.getStatus());
            return;
        }

        int returnQty = returnBatch.getReturnQuantity();
        int beforeAvailable = batch.getAvailableQuantity();
        int beforePreoccupied = batch.getPreoccupiedQuantity();
        int beforeSplit = batch.getSplitQuantity();
        int beforeDispensed = batch.getDispensedQuantity();

        if (Boolean.TRUE.equals(returnBatch.getFromSplit())) {
            batch.setSplitQuantity(beforeSplit + returnQty);
        } else {
            batch.setAvailableQuantity(beforeAvailable + returnQty);
        }
        batch.setDispensedQuantity(Math.max(0, beforeDispensed - returnQty));

        drugBatchRepository.save(batch);

        createBatchLog(batch, BatchLogType.RETURN_STOCK, returnQty,
                beforeAvailable, batch.getAvailableQuantity(),
                beforePreoccupied, beforePreoccupied,
                beforeSplit, batch.getSplitQuantity(),
                drugReturn.getPrescriptionNo(),
                "退药回库: " + (returnBatch.getFromSplit() ? "散片" : "整包"),
                drugReturn.getReviewedBy());
    }

    private void rejectReturn(DrugReturn drugReturn, String lossReason) {
        drugReturn.setStatus(DrugReturnStatus.REJECTED);
        drugReturn.setLossReason(lossReason);

        Map<String, Integer> drugLossQuantities = new HashMap<>();
        for (DrugReturnItem item : drugReturn.getItems()) {
            drugLossQuantities.merge(item.getDrugCode(), item.getReturnQuantity(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : drugLossQuantities.entrySet()) {
            String drugCode = entry.getKey();
            int lossQty = entry.getValue();

            Drug drug = drugRepository.findByDrugCode(drugCode).orElse(null);
            if (drug != null) {
                createInventoryLog(drug, InventoryLogType.RETURN_LOSS, lossQty,
                        drug.getAvailableStock(), drug.getAvailableStock(),
                        drug.getPreoccupiedStock(), drug.getPreoccupiedStock(),
                        drugReturn.getPrescriptionNo(),
                        "退药报损: " + (lossReason != null ? lossReason : ""),
                        drugReturn.getReviewedBy(),
                        null, null);
            }
        }

        for (DrugReturnItem item : drugReturn.getItems()) {
            for (DrugReturnItemBatch returnBatch : item.getBatchAllocations()) {
                DrugBatch batch = drugBatchRepository.findById(returnBatch.getBatchId()).orElse(null);
                if (batch != null) {
                    createBatchLog(batch, BatchLogType.RETURN_LOSS, returnBatch.getReturnQuantity(),
                            batch.getAvailableQuantity(), batch.getAvailableQuantity(),
                            batch.getPreoccupiedQuantity(), batch.getPreoccupiedQuantity(),
                            batch.getSplitQuantity(), batch.getSplitQuantity(),
                            drugReturn.getPrescriptionNo(),
                            "退药报损: " + (lossReason != null ? lossReason : ""),
                            drugReturn.getReviewedBy());
                }
            }
        }
    }

    public DrugReturnDTO getReturn(String returnNo) {
        DrugReturn drugReturn = drugReturnRepository.findByReturnNo(returnNo)
                .orElseThrow(() -> new ResourceNotFoundException("退药单不存在: " + returnNo));
        return DrugReturnDTO.fromEntity(drugReturn);
    }

    public List<DrugReturnDTO> getReturnsByPrescription(String prescriptionNo) {
        List<DrugReturn> returns = drugReturnRepository.findByPrescriptionNoOrderByCreatedAtDesc(prescriptionNo);
        return returns.stream().map(DrugReturnDTO::fromEntity).toList();
    }

    public List<DrugReturnDTO> getReturnsByPatient(String patientId) {
        List<DrugReturn> returns = drugReturnRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return returns.stream().map(DrugReturnDTO::fromEntity).toList();
    }

    public Page<DrugReturnDTO> getReturnsByStatus(DrugReturnStatus status, Pageable pageable) {
        Page<DrugReturn> returnPage = drugReturnRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return returnPage.map(DrugReturnDTO::fromEntity);
    }

    public Page<DrugReturnDTO> getAllReturns(Pageable pageable) {
        Page<DrugReturn> returnPage = drugReturnRepository.findAllByOrderByCreatedAtDesc(pageable);
        return returnPage.map(DrugReturnDTO::fromEntity);
    }

    public List<DrugReturnItemBatchDTO> getReturnBatchesByBatchNo(String batchNo) {
        List<DrugReturnItemBatch> batches =
                drugReturnItemBatchRepository.findApprovedReturnsByBatchNo(batchNo);
        return batches.stream().map(batch -> {
            DrugReturnItemBatchDTO dto = new DrugReturnItemBatchDTO();
            dto.setId(batch.getId());
            dto.setBatchId(batch.getBatchId());
            dto.setDrugCode(batch.getDrugCode());
            dto.setBatchNo(batch.getBatchNo());
            dto.setReturnQuantity(batch.getReturnQuantity());
            dto.setOriginalDispensedQuantity(batch.getOriginalDispensedQuantity());
            dto.setFromSplit(batch.getFromSplit());
            dto.setPending(batch.getPending());
            dto.setPendingReason(batch.getPendingReason());
            return dto;
        }).toList();
    }

    public List<DrugReturnItemBatchDTO> getPendingReturnBatches() {
        List<DrugReturnItemBatch> batches = drugReturnItemBatchRepository.findPendingReturnBatches();
        return batches.stream().map(batch -> {
            DrugReturnItemBatchDTO dto = new DrugReturnItemBatchDTO();
            dto.setId(batch.getId());
            dto.setBatchId(batch.getBatchId());
            dto.setDrugCode(batch.getDrugCode());
            dto.setBatchNo(batch.getBatchNo());
            dto.setReturnQuantity(batch.getReturnQuantity());
            dto.setOriginalDispensedQuantity(batch.getOriginalDispensedQuantity());
            dto.setFromSplit(batch.getFromSplit());
            dto.setPending(batch.getPending());
            dto.setPendingReason(batch.getPendingReason());
            return dto;
        }).toList();
    }

    public List<DrugReturnItemBatchDTO> getPendingReturnsByDrugCode(String drugCode) {
        List<DrugReturnItemBatch> batches =
                drugReturnItemBatchRepository.findPendingReturnsByDrugCode(drugCode);
        return batches.stream().map(batch -> {
            DrugReturnItemBatchDTO dto = new DrugReturnItemBatchDTO();
            dto.setId(batch.getId());
            dto.setBatchId(batch.getBatchId());
            dto.setDrugCode(batch.getDrugCode());
            dto.setBatchNo(batch.getBatchNo());
            dto.setReturnQuantity(batch.getReturnQuantity());
            dto.setOriginalDispensedQuantity(batch.getOriginalDispensedQuantity());
            dto.setFromSplit(batch.getFromSplit());
            dto.setPending(batch.getPending());
            dto.setPendingReason(batch.getPendingReason());
            return dto;
        }).toList();
    }

    private String generateReturnNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "RT" + timestamp + random;
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

    private void createBatchLog(DrugBatch batch, BatchLogType type, int quantity,
                                int beforeAvailable, int afterAvailable,
                                int beforePreoccupied, int afterPreoccupied,
                                int beforeSplit, int afterSplit,
                                String prescriptionNo, String remark, String operator) {
        BatchInventoryLog log = new BatchInventoryLog();
        log.setBatchId(batch.getId());
        log.setDrugCode(batch.getDrugCode());
        log.setDrugName(batch.getDrugName());
        log.setBatchNo(batch.getBatchNo());
        log.setLogType(type);
        log.setQuantity(quantity);
        log.setBeforeAvailable(beforeAvailable);
        log.setAfterAvailable(afterAvailable);
        log.setBeforePreoccupied(beforePreoccupied);
        log.setAfterPreoccupied(afterPreoccupied);
        log.setBeforeSplit(beforeSplit);
        log.setAfterSplit(afterSplit);
        log.setPrescriptionNo(prescriptionNo);
        log.setRemark(remark);
        log.setOperator(operator);
        batchInventoryLogRepository.save(log);
    }
}
