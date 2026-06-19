package com.pharmacy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.dto.*;
import com.pharmacy.entity.*;
import com.pharmacy.enums.BatchLogType;
import com.pharmacy.enums.BatchStatus;
import com.pharmacy.enums.ExpiryWarningLevel;
import com.pharmacy.enums.InventoryLogType;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private static final List<BatchStatus> EXCLUDED_BATCH_STATUSES = List.of(
            BatchStatus.LOCKED, BatchStatus.RECALLED, BatchStatus.EXPIRED);

    private final DrugRepository drugRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugSplitRecordRepository drugSplitRecordRepository;
    private final BatchInventoryLogRepository batchInventoryLogRepository;
    private final PrescriptionItemBatchRepository prescriptionItemBatchRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DrugBatchDTO stockIn(BatchStockInDTO dto) {
        log.info("批次入库: 药品[{}], 批号[{}], 数量[{}]", dto.getDrugCode(), dto.getBatchNo(), dto.getQuantity());

        if (dto.getExpiryDate().isBefore(dto.getProductionDate())) {
            throw new BusinessException("有效期不能早于生产日期");
        }
        if (dto.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException("该批次已过期，无法入库");
        }

        Drug drug = drugRepository.findByDrugCodeWithLock(dto.getDrugCode())
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + dto.getDrugCode()));

        Optional<DrugBatch> existingBatch = drugBatchRepository.findByDrugCodeAndBatchNo(
                dto.getDrugCode(), dto.getBatchNo());
        if (existingBatch.isPresent()) {
            throw new BusinessException(
                    String.format("药品[%s]已存在批号[%s]的批次，请使用盘点调整",
                            drug.getName(), dto.getBatchNo()));
        }

        int packageQty = 1;
        if (Boolean.TRUE.equals(drug.getSplittable()) && drug.getPackageQuantity() != null) {
            packageQty = drug.getPackageQuantity();
        }
        int totalQuantityInMinUnit = dto.getQuantity() * packageQty;

        DrugBatch batch = new DrugBatch();
        batch.setDrugCode(drug.getDrugCode());
        batch.setDrugName(drug.getName());
        batch.setBatchNo(dto.getBatchNo());
        batch.setProductionDate(dto.getProductionDate());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setPurchasePrice(dto.getPurchasePrice());
        batch.setTotalQuantity(totalQuantityInMinUnit);
        batch.setAvailableQuantity(totalQuantityInMinUnit);
        batch.setPreoccupiedQuantity(0);
        batch.setSplitQuantity(0);
        batch.setDispensedQuantity(0);
        batch.setStatus(calculateInitialStatus(dto.getExpiryDate()));
        batch.setRemark(dto.getRemark());
        batch.setOperator(dto.getOperator());

        batch = drugBatchRepository.save(batch);

        int beforeAvailable = drug.getAvailableStock();
        int beforePreoccupied = drug.getPreoccupiedStock();
        drug.setAvailableStock(beforeAvailable + totalQuantityInMinUnit);
        drugRepository.save(drug);

        createInventoryLog(drug, InventoryLogType.STOCK_IN, totalQuantityInMinUnit,
                beforeAvailable, beforeAvailable + totalQuantityInMinUnit,
                beforePreoccupied, beforePreoccupied,
                null, "批次入库: " + dto.getBatchNo() + " (包装数量:" + dto.getQuantity() + ")", dto.getOperator(),
                dto.getBatchNo(), null);

        createBatchLog(batch, BatchLogType.STOCK_IN, totalQuantityInMinUnit,
                0, totalQuantityInMinUnit,
                0, 0,
                0, 0,
                null, "批次入库: 包装数量" + dto.getQuantity(), dto.getOperator());

        log.info("批次入库完成: 批次ID[{}], 包装数量[{}], 最小单位数量[{}]",
                batch.getId(), dto.getQuantity(), totalQuantityInMinUnit);
        return convertToBatchDTO(batch);
    }

    public List<DrugBatchDTO> getBatchesByDrugCode(String drugCode) {
        List<DrugBatch> batches = drugBatchRepository.findByDrugCodeOrderByExpiryDateAsc(drugCode);
        return batches.stream().map(this::convertToBatchDTO).toList();
    }

    public DrugBatchDTO getBatchById(Long batchId) {
        DrugBatch batch = drugBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("批次不存在: " + batchId));
        return convertToBatchDTO(batch);
    }

    public BatchLifecycleDTO getBatchLifecycle(Long batchId) {
        DrugBatch batch = drugBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("批次不存在: " + batchId));

        BatchLifecycleDTO lifecycle = new BatchLifecycleDTO();
        lifecycle.setBatchId(batch.getId());
        lifecycle.setDrugCode(batch.getDrugCode());
        lifecycle.setDrugName(batch.getDrugName());
        lifecycle.setBatchNo(batch.getBatchNo());
        lifecycle.setProductionDate(batch.getProductionDate());
        lifecycle.setExpiryDate(batch.getExpiryDate());
        lifecycle.setTotalQuantity(batch.getTotalQuantity());
        lifecycle.setRemainingQuantity(batch.getRemainingQuantity());
        lifecycle.setStatus(batch.getStatus());
        lifecycle.setWarningLevel(calculateWarningLevel(batch.getExpiryDate()));

        List<BatchInventoryLog> logs = batchInventoryLogRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
        lifecycle.setLogs(logs.stream().map(this::convertToBatchLogDTO).toList());

        return lifecycle;
    }

    public List<ExpiryWarningDTO> getExpiryWarnings(ExpiryWarningLevel level, String drugCode) {
        LocalDate today = LocalDate.now();
        LocalDate yellowThreshold = today.plusDays(30);
        LocalDate redThreshold = today.plusDays(7);

        List<DrugBatch> candidateBatches;

        if (drugCode != null && !drugCode.isBlank()) {
            candidateBatches = drugBatchRepository.findByDrugCodeOrderByExpiryDateAsc(drugCode);
        } else {
            if (level == ExpiryWarningLevel.RED) {
                candidateBatches = drugBatchRepository.findBatchesExpiringBetween(today, redThreshold, EXCLUDED_BATCH_STATUSES);
            } else if (level == ExpiryWarningLevel.YELLOW) {
                candidateBatches = drugBatchRepository.findBatchesExpiringBetween(redThreshold.plusDays(1), yellowThreshold, EXCLUDED_BATCH_STATUSES);
            } else {
                candidateBatches = drugBatchRepository.findBatchesExpiringBefore(yellowThreshold, EXCLUDED_BATCH_STATUSES);
            }
        }

        return candidateBatches.stream()
                .map(b -> {
                    ExpiryWarningLevel wl = calculateWarningLevel(b.getExpiryDate());
                    if (level != null && level != ExpiryWarningLevel.NONE && wl != level) {
                        return null;
                    }
                    if (wl == ExpiryWarningLevel.NONE) {
                        return null;
                    }
                    ExpiryWarningDTO dto = new ExpiryWarningDTO();
                    dto.setBatchId(b.getId());
                    dto.setDrugCode(b.getDrugCode());
                    dto.setDrugName(b.getDrugName());
                    dto.setBatchNo(b.getBatchNo());
                    dto.setExpiryDate(b.getExpiryDate());
                    dto.setDaysToExpiry(ChronoUnit.DAYS.between(today, b.getExpiryDate()));
                    dto.setRemainingQuantity(b.getRemainingQuantity());
                    dto.setWarningLevel(wl);
                    return dto;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ExpiryWarningDTO::getDaysToExpiry))
                .toList();
    }

    @Transactional
    public void scanAndUpdateExpiryStatus() {
        log.info("开始执行效期扫描任务");
        LocalDate today = LocalDate.now();
        LocalDate redThreshold = today.plusDays(7);
        LocalDate yellowThreshold = today.plusDays(30);

        List<DrugBatch> allBatches = drugBatchRepository.findByStatusIn(
                List.of(BatchStatus.NORMAL, BatchStatus.NEAR_EXPIRY, BatchStatus.ABOUT_TO_EXPIRE));

        int updatedCount = 0;
        for (DrugBatch batch : allBatches) {
            BatchStatus newStatus = calculateInitialStatus(batch.getExpiryDate());
            if (newStatus != batch.getStatus()) {
                if (newStatus == BatchStatus.EXPIRED) {
                    Drug drug = drugRepository.findByDrugCodeWithLock(batch.getDrugCode()).orElse(null);
                    if (drug != null) {
                        int beforeAvailable = drug.getAvailableStock();
                        int beforePreoccupied = drug.getPreoccupiedStock();
                        int reduceQty = batch.getAvailableQuantity() + batch.getSplitQuantity();
                        drug.setAvailableStock(Math.max(0, beforeAvailable - reduceQty));
                        drugRepository.save(drug);
                        createInventoryLog(drug, InventoryLogType.ADJUST, -reduceQty,
                                beforeAvailable, drug.getAvailableStock(),
                                beforePreoccupied, beforePreoccupied,
                                null, "过期批次锁定，从可发药池移除", "system",
                                batch.getBatchNo(), null);
                    }
                    int batchBeforeAvailable = batch.getAvailableQuantity();
                    int batchBeforeSplit = batch.getSplitQuantity();
                    batch.setAvailableQuantity(0);
                    batch.setSplitQuantity(0);
                    createBatchLog(batch, BatchLogType.EXPIRE_LOCK, batchBeforeAvailable + batchBeforeSplit,
                            batchBeforeAvailable, 0,
                            batch.getPreoccupiedQuantity(), batch.getPreoccupiedQuantity(),
                            batchBeforeSplit, 0,
                            null, "批次过期自动锁定", "system");
                }
                batch.setStatus(newStatus);
                drugBatchRepository.save(batch);
                updatedCount++;
                log.info("批次[{}]状态更新: {} -> {}", batch.getBatchNo(), batch.getStatus(), newStatus);
            }
        }
        log.info("效期扫描任务完成，更新了{}个批次", updatedCount);
    }

    @Transactional
    public BatchRecallResultDTO recallBatch(BatchRecallDTO dto) {
        log.info("执行批次召回: 批号[{}], 原因[{}]", dto.getBatchNo(), dto.getReason());

        List<DrugBatch> batches = drugBatchRepository.findAllByBatchNo(dto.getBatchNo());
        if (batches.isEmpty()) {
            throw new ResourceNotFoundException("未找到批号为[" + dto.getBatchNo() + "]的批次");
        }

        int lockedQty = 0;
        for (DrugBatch batch : batches) {
            if (batch.getStatus() != BatchStatus.RECALLED) {
                int beforeAvailable = batch.getAvailableQuantity();
                int beforeSplit = batch.getSplitQuantity();
                lockedQty += beforeAvailable + beforeSplit;

                Drug drug = drugRepository.findByDrugCodeWithLock(batch.getDrugCode()).orElse(null);
                if (drug != null) {
                    int drugBeforeAvailable = drug.getAvailableStock();
                    int drugBeforePreoccupied = drug.getPreoccupiedStock();
                    drug.setAvailableStock(Math.max(0, drugBeforeAvailable - beforeAvailable - beforeSplit));
                    drugRepository.save(drug);
                    createInventoryLog(drug, InventoryLogType.ADJUST, -(beforeAvailable + beforeSplit),
                            drugBeforeAvailable, drug.getAvailableStock(),
                            drugBeforePreoccupied, drugBeforePreoccupied,
                            null, "批次召回锁定: " + dto.getReason(), dto.getOperator(),
                            batch.getBatchNo(), null);
                }

                batch.setAvailableQuantity(0);
                batch.setSplitQuantity(0);
                batch.setStatus(BatchStatus.RECALLED);
                drugBatchRepository.save(batch);

                createBatchLog(batch, BatchLogType.RECALL, beforeAvailable + beforeSplit,
                        beforeAvailable, 0,
                        batch.getPreoccupiedQuantity(), batch.getPreoccupiedQuantity(),
                        beforeSplit, 0,
                        null, "批次召回: " + dto.getReason(), dto.getOperator());
            }
        }

        List<PrescriptionItemBatch> dispensedAllocations =
                prescriptionItemBatchRepository.findByBatchNoAndPrescriptionStatus(
                        dto.getBatchNo(), PrescriptionStatus.DISPENSED);

        Map<String, AffectedPatientDTO> patientMap = new LinkedHashMap<>();
        Set<String> prescriptionNos = new HashSet<>();

        for (PrescriptionItemBatch allocation : dispensedAllocations) {
            PrescriptionItem item = allocation.getPrescriptionItem();
            Prescription prescription = item.getPrescription();
            prescriptionNos.add(prescription.getPrescriptionNo());

            String key = prescription.getPatientId() + "_" + prescription.getPrescriptionNo();
            AffectedPatientDTO affected = patientMap.computeIfAbsent(key, k -> {
                AffectedPatientDTO a = new AffectedPatientDTO();
                a.setPatientId(prescription.getPatientId());
                a.setPatientName(prescription.getPatientName());
                a.setPrescriptionNo(prescription.getPrescriptionNo());
                a.setDispensedAt(prescription.getDispensedAt());
                a.setDispensedBy(prescription.getDispensedBy());
                a.setDispensedQuantity(0);
                return a;
            });
            affected.setDispensedQuantity(affected.getDispensedQuantity() + allocation.getQuantity());
        }

        BatchRecallResultDTO result = new BatchRecallResultDTO();
        result.setBatchNo(dto.getBatchNo());
        result.setLockedRemainingQuantity(lockedQty);
        result.setAffectedPatientCount((int) patientMap.values().stream()
                .map(AffectedPatientDTO::getPatientId)
                .distinct().count());
        result.setAffectedPrescriptionCount(prescriptionNos.size());
        result.setAffectedPatients(new ArrayList<>(patientMap.values()));

        log.info("批次召回完成: 批号[{}], 锁定数量[{}], 影响患者[{}]人, 影响处方[{}]张",
                dto.getBatchNo(), lockedQty, result.getAffectedPatientCount(), result.getAffectedPrescriptionCount());

        return result;
    }

    @Transactional
    public DrugSplitRecordDTO performSplit(SplitOperationDTO dto) {
        log.info("执行拆零操作: 批次ID[{}], 拆零数量[{}]", dto.getBatchId(), dto.getSplitQuantity());

        DrugBatch batch = drugBatchRepository.findSplittableBatchByIdWithLock(dto.getBatchId())
                .orElseThrow(() -> new BusinessException("该批次不可拆零或已被锁定"));

        Drug drug = drugRepository.findByDrugCode(batch.getDrugCode())
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + batch.getDrugCode()));

        if (!Boolean.TRUE.equals(drug.getSplittable())) {
            throw new BusinessException("该药品未配置为可拆零");
        }
        if (drug.getPackageQuantity() == null || drug.getPackageQuantity() <= 0) {
            throw new BusinessException("该药品未正确配置拆零规格(包装数量)");
        }

        int splitUnit = drug.getPackageQuantity();
        int splitQty = dto.getSplitQuantity();

        if (splitQty != splitUnit) {
            throw new BusinessException(
                    String.format("拆零数量必须等于包装规格%d%s", splitUnit, drug.getSplitUnit() != null ? drug.getSplitUnit() : ""));
        }

        if (batch.getAvailableQuantity() < splitUnit) {
            throw new BusinessException("该批次可用库存不足，无法拆零");
        }

        Optional<DrugSplitRecord> existingActive = drugSplitRecordRepository.findByBatchIdAndActiveTrue(dto.getBatchId());
        if (existingActive.isPresent()) {
            throw new BusinessException("该批次已有进行中的拆零记录，请先完成或关闭");
        }

        int beforeAvailable = batch.getAvailableQuantity();
        int beforeSplit = batch.getSplitQuantity();

        batch.setAvailableQuantity(beforeAvailable - splitUnit);
        batch.setSplitQuantity(beforeSplit + splitQty);
        batch.setSplitLocked(true);
        batch.setSplitLockedBy(dto.getOperator());
        batch.setSplitLockedAt(LocalDateTime.now());
        drugBatchRepository.save(batch);

        DrugSplitRecord splitRecord = new DrugSplitRecord();
        splitRecord.setBatchId(batch.getId());
        splitRecord.setDrugCode(batch.getDrugCode());
        splitRecord.setDrugName(batch.getDrugName());
        splitRecord.setBatchNo(batch.getBatchNo());
        splitRecord.setPackageQuantity(1);
        splitRecord.setSplitUnit(splitUnit);
        splitRecord.setSplitQuantity(splitQty);
        splitRecord.setDispensedSplitQuantity(0);
        splitRecord.setRemainingSplitQuantity(splitQty);
        splitRecord.setOperator(dto.getOperator());
        splitRecord.setRemark(dto.getRemark());
        splitRecord = drugSplitRecordRepository.save(splitRecord);

        createBatchLog(batch, BatchLogType.SPLIT, splitQty,
                beforeAvailable, batch.getAvailableQuantity(),
                batch.getPreoccupiedQuantity(), batch.getPreoccupiedQuantity(),
                beforeSplit, batch.getSplitQuantity(),
                null, "拆零操作: " + (dto.getRemark() != null ? dto.getRemark() : ""), dto.getOperator());

        log.info("拆零完成: 拆零记录ID[{}]", splitRecord.getId());
        return convertToSplitDTO(splitRecord);
    }

    @Transactional
    public void releaseSplitLock(Long batchId, String operator) {
        log.info("释放拆零锁: 批次ID[{}]", batchId);
        DrugBatch batch = drugBatchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("批次不存在: " + batchId));

        if (!Boolean.TRUE.equals(batch.getSplitLocked())) {
            log.info("批次[{}]未被锁定，无需释放", batchId);
            return;
        }

        Drug drug = drugRepository.findByDrugCode(batch.getDrugCode()).orElse(null);
        int packageUnit = (drug != null && drug.getPackageQuantity() != null) ? drug.getPackageQuantity() : 1;

        List<DrugSplitRecord> activeRecords = drugSplitRecordRepository.findActiveSplitRecordsByBatchId(batchId);
        for (DrugSplitRecord record : activeRecords) {
            if (record.getRemainingSplitQuantity() > 0) {
                int revertQty = record.getRemainingSplitQuantity();
                int beforeAvailable = batch.getAvailableQuantity();
                int beforeSplit = batch.getSplitQuantity();
                batch.setAvailableQuantity(beforeAvailable + packageUnit);
                batch.setSplitQuantity(Math.max(0, beforeSplit - revertQty));
                createBatchLog(batch, BatchLogType.ADJUST, revertQty,
                        beforeAvailable, batch.getAvailableQuantity(),
                        batch.getPreoccupiedQuantity(), batch.getPreoccupiedQuantity(),
                        beforeSplit, batch.getSplitQuantity(),
                        null, "拆零回退: 剩余散片回归库存", operator);
            }
            record.setActive(false);
            record.setClosedAt(LocalDateTime.now());
            drugSplitRecordRepository.save(record);
        }

        batch.setSplitLocked(false);
        batch.setSplitLockedBy(null);
        batch.setSplitLockedAt(null);
        drugBatchRepository.save(batch);
    }

    public List<DrugSplitRecordDTO> getSplitRecordsByDrug(String drugCode) {
        return drugSplitRecordRepository.findByDrugCodeOrderByCreatedAtDesc(drugCode)
                .stream().map(this::convertToSplitDTO).toList();
    }

    public List<DrugSplitRecordDTO> getSplitRecordsByBatch(Long batchId) {
        return drugSplitRecordRepository.findByBatchIdOrderByCreatedAtDesc(batchId)
                .stream().map(this::convertToSplitDTO).toList();
    }

    public List<PatientDrugTraceDTO> getPatientDrugTrace(String patientId) {
        List<PrescriptionItemBatch> allocations = prescriptionItemBatchRepository
                .findByPatientIdAndPrescriptionStatus(patientId, PrescriptionStatus.DISPENSED);

        return allocations.stream().map(a -> {
            Prescription prescription = a.getPrescriptionItem().getPrescription();
            PatientDrugTraceDTO dto = new PatientDrugTraceDTO();
            dto.setPatientId(prescription.getPatientId());
            dto.setPatientName(prescription.getPatientName());
            dto.setPrescriptionNo(prescription.getPrescriptionNo());
            dto.setDrugCode(a.getDrugCode());
            dto.setDrugName(a.getPrescriptionItem().getDrugName());
            dto.setBatchNo(a.getBatchNo());
            dto.setQuantity(a.getQuantity());
            dto.setFromSplit(a.getFromSplit());
            dto.setDispensedAt(prescription.getDispensedAt());
            dto.setDispensedBy(prescription.getDispensedBy());
            return dto;
        }).toList();
    }

    public static class BatchAllocation {
        public Long batchId;
        public String batchNo;
        public int quantity;
        public boolean fromSplit;
        public Long splitRecordId;
    }

    @Transactional
    public List<BatchAllocation> allocateBatchesForPreoccupy(String drugCode, int requiredQty,
                                                              String prescriptionNo) {
        log.debug("为药品[{}]分配批次预占，需要数量: {}", drugCode, requiredQty);

        Drug drug = drugRepository.findByDrugCode(drugCode)
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + drugCode));

        List<BatchAllocation> allocations = new ArrayList<>();
        int remaining = requiredQty;

        if (Boolean.TRUE.equals(drug.getSplittable())) {
            List<DrugSplitRecord> activeSplits =
                    drugSplitRecordRepository.findActiveSplitRecordsByDrugCode(drugCode);
            for (DrugSplitRecord split : activeSplits) {
                if (remaining <= 0) break;
                int usable = Math.min(split.getRemainingSplitQuantity(), remaining);
                if (usable > 0) {
                    BatchAllocation alloc = new BatchAllocation();
                    alloc.batchId = split.getBatchId();
                    alloc.batchNo = split.getBatchNo();
                    alloc.quantity = usable;
                    alloc.fromSplit = true;
                    alloc.splitRecordId = split.getId();
                    allocations.add(alloc);
                    remaining -= usable;
                }
            }
        }

        if (remaining > 0) {
            List<DrugBatch> availableBatches =
                    drugBatchRepository.findAvailableBatchesByDrugCodeWithLock(drugCode, EXCLUDED_BATCH_STATUSES);

            for (DrugBatch batch : availableBatches) {
                if (remaining <= 0) break;

                int batchUsable = batch.getAvailableTotal();
                if (batchUsable <= 0) continue;

                int takeQty = Math.min(batchUsable, remaining);

                BatchAllocation alloc = new BatchAllocation();
                alloc.batchId = batch.getId();
                alloc.batchNo = batch.getBatchNo();
                alloc.quantity = takeQty;
                alloc.fromSplit = (batch.getSplitQuantity() >= takeQty);
                allocations.add(alloc);
                remaining -= takeQty;
            }
        }

        if (remaining > 0) {
            log.warn("药品[{}]批次库存不足，需要[{}]，可分配[{}]",
                    drugCode, requiredQty, requiredQty - remaining);
        }

        return allocations;
    }

    @Transactional
    public void applyPreoccupyAllocations(String drugCode, String prescriptionNo,
                                           List<BatchAllocation> allocations, String operator) {
        Drug drug = drugRepository.findByDrugCodeWithLock(drugCode)
                .orElseThrow(() -> new ResourceNotFoundException("药品不存在: " + drugCode));

        for (BatchAllocation alloc : allocations) {
            if (alloc.fromSplit && alloc.splitRecordId != null) {
                DrugSplitRecord splitRecord = drugSplitRecordRepository.findById(alloc.splitRecordId)
                        .orElseThrow(() -> new ResourceNotFoundException("拆零记录不存在: " + alloc.splitRecordId));
                splitRecord.setRemainingSplitQuantity(splitRecord.getRemainingSplitQuantity() - alloc.quantity);
                splitRecord.setDispensedSplitQuantity(splitRecord.getDispensedSplitQuantity() + alloc.quantity);
                if (splitRecord.getRemainingSplitQuantity() <= 0) {
                    splitRecord.setActive(false);
                    splitRecord.setClosedAt(LocalDateTime.now());
                }
                drugSplitRecordRepository.save(splitRecord);

                DrugBatch batch = drugBatchRepository.findByIdWithLock(alloc.batchId)
                        .orElseThrow(() -> new ResourceNotFoundException("批次不存在: " + alloc.batchId));
                int beforeSplit = batch.getSplitQuantity();
                batch.setSplitQuantity(beforeSplit - alloc.quantity);
                batch.setPreoccupiedQuantity(batch.getPreoccupiedQuantity() + alloc.quantity);
                drugBatchRepository.save(batch);

                createBatchLog(batch, BatchLogType.PREOCCUPY, alloc.quantity,
                        batch.getAvailableQuantity(), batch.getAvailableQuantity(),
                        batch.getPreoccupiedQuantity() - alloc.quantity, batch.getPreoccupiedQuantity(),
                        beforeSplit, batch.getSplitQuantity(),
                        prescriptionNo, "拆零散片预占", operator);
            } else {
                DrugBatch batch = drugBatchRepository.findByIdWithLock(alloc.batchId)
                        .orElseThrow(() -> new ResourceNotFoundException("批次不存在: " + alloc.batchId));

                int qtyToPreoccupy = alloc.quantity;
                int beforeAvailable = batch.getAvailableQuantity();
                int beforeSplit = batch.getSplitQuantity();
                int beforePreoccupied = batch.getPreoccupiedQuantity();

                int takeFromSplit = 0;
                int takeFromAvailable = 0;

                if (batch.getSplitQuantity() > 0) {
                    takeFromSplit = Math.min(batch.getSplitQuantity(), qtyToPreoccupy);
                    batch.setSplitQuantity(batch.getSplitQuantity() - takeFromSplit);
                    qtyToPreoccupy -= takeFromSplit;
                }

                if (qtyToPreoccupy > 0 && batch.getAvailableQuantity() > 0) {
                    takeFromAvailable = Math.min(batch.getAvailableQuantity(), qtyToPreoccupy);
                    batch.setAvailableQuantity(batch.getAvailableQuantity() - takeFromAvailable);
                    qtyToPreoccupy -= takeFromAvailable;
                }

                if (qtyToPreoccupy > 0) {
                    throw new BusinessException(
                            String.format("批次[%s]可用库存不足，需要[%d]，剩余可用[%d]",
                                    batch.getBatchNo(), alloc.quantity, batch.getAvailableTotal()));
                }

                batch.setPreoccupiedQuantity(beforePreoccupied + alloc.quantity);
                drugBatchRepository.save(batch);

                String remark = "批次预占";
                if (takeFromSplit > 0 && takeFromAvailable > 0) {
                    remark = String.format("批次预占: 散片%d + 整包%d", takeFromSplit, takeFromAvailable);
                } else if (takeFromSplit > 0) {
                    remark = "批次预占: 散片";
                }

                createBatchLog(batch, BatchLogType.PREOCCUPY, alloc.quantity,
                        beforeAvailable, batch.getAvailableQuantity(),
                        beforePreoccupied, batch.getPreoccupiedQuantity(),
                        beforeSplit, batch.getSplitQuantity(),
                        prescriptionNo, remark, operator);
            }
        }
    }

    @Transactional
    public void releasePreoccupyAllocations(String drugCode, String prescriptionNo, String reason, String operator) {
        List<BatchInventoryLog> preoccupyLogs = batchInventoryLogRepository
                .findByPrescriptionNoAndLogType(prescriptionNo, BatchLogType.PREOCCUPY);

        for (BatchInventoryLog preLog : preoccupyLogs) {
            DrugBatch batch = drugBatchRepository.findByIdWithLock(preLog.getBatchId()).orElse(null);
            if (batch == null) continue;

            int releaseQty = preLog.getQuantity();
            int beforeAvailable = batch.getAvailableQuantity();
            int beforeSplit = batch.getSplitQuantity();
            int beforePreoccupied = batch.getPreoccupiedQuantity();

            int availableReduced = preLog.getBeforeAvailable() - preLog.getAfterAvailable();
            int splitReduced = preLog.getBeforeSplit() != null && preLog.getAfterSplit() != null
                    ? preLog.getBeforeSplit() - preLog.getAfterSplit() : 0;

            if (availableReduced > 0) {
                batch.setAvailableQuantity(beforeAvailable + availableReduced);
            }
            if (splitReduced > 0) {
                batch.setSplitQuantity(beforeSplit + splitReduced);
            }

            batch.setPreoccupiedQuantity(Math.max(0, beforePreoccupied - releaseQty));

            drugBatchRepository.save(batch);

            createBatchLog(batch, BatchLogType.RELEASE_PREOCCUPY, releaseQty,
                    beforeAvailable, batch.getAvailableQuantity(),
                    beforePreoccupied, batch.getPreoccupiedQuantity(),
                    beforeSplit, batch.getSplitQuantity(),
                    prescriptionNo, "释放预占: " + reason, operator);
        }
    }

    @Transactional
    public void applyDispenseAllocations(String drugCode, String prescriptionNo, String dispensedBy) {
        List<BatchInventoryLog> preoccupyLogs = batchInventoryLogRepository
                .findByPrescriptionNoAndLogType(prescriptionNo, BatchLogType.PREOCCUPY);

        for (BatchInventoryLog preLog : preoccupyLogs) {
            DrugBatch batch = drugBatchRepository.findByIdWithLock(preLog.getBatchId()).orElse(null);
            if (batch == null) continue;

            int dispenseQty = preLog.getQuantity();
            int beforePreoccupied = batch.getPreoccupiedQuantity();
            int beforeDispensed = batch.getDispensedQuantity();

            batch.setPreoccupiedQuantity(Math.max(0, beforePreoccupied - dispenseQty));
            batch.setDispensedQuantity(beforeDispensed + dispenseQty);
            drugBatchRepository.save(batch);

            createBatchLog(batch, BatchLogType.DISPENSE, dispenseQty,
                    batch.getAvailableQuantity(), batch.getAvailableQuantity(),
                    beforePreoccupied, batch.getPreoccupiedQuantity(),
                    batch.getSplitQuantity(), batch.getSplitQuantity(),
                    prescriptionNo, "批次发药", dispensedBy);
        }
    }

    String buildBatchDetailsJson(List<BatchAllocation> allocations) {
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

    private BatchStatus calculateInitialStatus(LocalDate expiryDate) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, expiryDate);
        if (days < 0) return BatchStatus.EXPIRED;
        if (days <= 7) return BatchStatus.ABOUT_TO_EXPIRE;
        if (days <= 30) return BatchStatus.NEAR_EXPIRY;
        return BatchStatus.NORMAL;
    }

    private ExpiryWarningLevel calculateWarningLevel(LocalDate expiryDate) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, expiryDate);
        if (days < 0) return ExpiryWarningLevel.NONE;
        if (days <= 7) return ExpiryWarningLevel.RED;
        if (days <= 30) return ExpiryWarningLevel.YELLOW;
        return ExpiryWarningLevel.NONE;
    }

    private DrugBatchDTO convertToBatchDTO(DrugBatch batch) {
        DrugBatchDTO dto = new DrugBatchDTO();
        dto.setId(batch.getId());
        dto.setDrugCode(batch.getDrugCode());
        dto.setDrugName(batch.getDrugName());
        dto.setBatchNo(batch.getBatchNo());
        dto.setProductionDate(batch.getProductionDate());
        dto.setExpiryDate(batch.getExpiryDate());
        dto.setPurchasePrice(batch.getPurchasePrice());
        dto.setTotalQuantity(batch.getTotalQuantity());
        dto.setAvailableQuantity(batch.getAvailableQuantity());
        dto.setPreoccupiedQuantity(batch.getPreoccupiedQuantity());
        dto.setSplitQuantity(batch.getSplitQuantity());
        dto.setDispensedQuantity(batch.getDispensedQuantity());
        dto.setRemainingQuantity(batch.getRemainingQuantity());
        dto.setStatus(batch.getStatus());
        dto.setWarningLevel(calculateWarningLevel(batch.getExpiryDate()));
        dto.setDaysToExpiry(ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate()));
        dto.setSplitLocked(batch.getSplitLocked());
        dto.setSplitLockedBy(batch.getSplitLockedBy());
        dto.setSplitLockedAt(batch.getSplitLockedAt());
        dto.setRemark(batch.getRemark());
        dto.setOperator(batch.getOperator());
        dto.setCreatedAt(batch.getCreatedAt());
        dto.setUpdatedAt(batch.getUpdatedAt());
        return dto;
    }

    private DrugSplitRecordDTO convertToSplitDTO(DrugSplitRecord record) {
        DrugSplitRecordDTO dto = new DrugSplitRecordDTO();
        dto.setId(record.getId());
        dto.setBatchId(record.getBatchId());
        dto.setDrugCode(record.getDrugCode());
        dto.setDrugName(record.getDrugName());
        dto.setBatchNo(record.getBatchNo());
        dto.setPackageQuantity(record.getPackageQuantity());
        dto.setSplitUnit(record.getSplitUnit());
        dto.setSplitQuantity(record.getSplitQuantity());
        dto.setDispensedSplitQuantity(record.getDispensedSplitQuantity());
        dto.setRemainingSplitQuantity(record.getRemainingSplitQuantity());
        dto.setPrescriptionNo(record.getPrescriptionNo());
        dto.setOperator(record.getOperator());
        dto.setRemark(record.getRemark());
        dto.setActive(record.getActive());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setClosedAt(record.getClosedAt());
        return dto;
    }

    private BatchInventoryLogDTO convertToBatchLogDTO(BatchInventoryLog log) {
        BatchInventoryLogDTO dto = new BatchInventoryLogDTO();
        dto.setId(log.getId());
        dto.setBatchId(log.getBatchId());
        dto.setDrugCode(log.getDrugCode());
        dto.setDrugName(log.getDrugName());
        dto.setBatchNo(log.getBatchNo());
        dto.setLogType(log.getLogType());
        dto.setLogTypeDescription(log.getLogType().getDescription());
        dto.setQuantity(log.getQuantity());
        dto.setBeforeAvailable(log.getBeforeAvailable());
        dto.setAfterAvailable(log.getAfterAvailable());
        dto.setBeforePreoccupied(log.getBeforePreoccupied());
        dto.setAfterPreoccupied(log.getAfterPreoccupied());
        dto.setBeforeSplit(log.getBeforeSplit());
        dto.setAfterSplit(log.getAfterSplit());
        dto.setPrescriptionNo(log.getPrescriptionNo());
        dto.setRemark(log.getRemark());
        dto.setOperator(log.getOperator());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
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
