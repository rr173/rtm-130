package com.pharmacy.service;

import com.pharmacy.dto.pharmacistreview.*;
import com.pharmacy.entity.PharmacistReviewRecord;
import com.pharmacy.entity.Prescription;
import com.pharmacy.entity.PrescriptionItem;
import com.pharmacy.entity.ReviewTimeoutEvent;
import com.pharmacy.enums.PharmacistReviewConclusion;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.enums.ReviewResultType;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacistReviewService {

    private final PrescriptionRepository prescriptionRepository;
    private final PharmacistRepository pharmacistRepository;
    private final PharmacistReviewRecordRepository reviewRecordRepository;
    private final ReviewTimeoutEventRepository timeoutEventRepository;
    private final DrugRepository drugRepository;
    private final InventoryService inventoryService;
    private final DispenseQueueService dispenseQueueService;

    private static final int NORMAL_REVIEW_MINUTES = 30;
    private static final int EMERGENCY_REVIEW_MINUTES = 10;

    public Page<ReviewTodoItemDTO> getTodoPool(Pageable pageable) {
        Page<Prescription> prescriptionPage = prescriptionRepository.findTodoPoolPrescriptions(pageable);
        return prescriptionPage.map(this::convertToTodoItem);
    }

    public List<ReviewTodoItemDTO> getTodoPool() {
        List<Prescription> prescriptions = prescriptionRepository.findTodoPoolPrescriptions();
        return prescriptions.stream()
                .map(this::convertToTodoItem)
                .toList();
    }

    public List<ReviewTodoItemDTO> getMyInReviewPrescriptions(String pharmacistId) {
        List<Prescription> prescriptions = prescriptionRepository.findInReviewByPharmacist(pharmacistId);
        return prescriptions.stream()
                .map(this::convertToTodoItem)
                .toList();
    }

    @Transactional
    public ReviewTodoItemDTO claimPrescription(String prescriptionNo, String pharmacistId, String pharmacistName) {
        log.info("药师[{}]尝试领取处方[{}]", pharmacistId, prescriptionNo);

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));

        if (prescription.getStatus() != PrescriptionStatus.PENDING_PHARMACIST_REVIEW) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许领取", prescription.getStatus().getDescription()));
        }

        prescription.setStatus(PrescriptionStatus.IN_PHARMACIST_REVIEW);
        prescription.setClaimedByPharmacistId(pharmacistId);
        prescription.setClaimedByPharmacistName(pharmacistName != null ? pharmacistName : pharmacistId);
        prescription.setClaimedAt(LocalDateTime.now());
        prescription.setReviewDeadline(calculateReviewDeadline(prescription.getType()));

        prescription = prescriptionRepository.save(prescription);
        log.info("处方[{}]已被药师[{}]领取，审方截止时间: {}",
                prescriptionNo, pharmacistId, prescription.getReviewDeadline());

        return convertToTodoItem(prescription);
    }

    @Transactional
    public ReviewTodoItemDTO claimNextPrescription(String pharmacistId, String pharmacistName) {
        log.info("药师[{}]尝试领取下一张处方", pharmacistId);

        List<Prescription> todoList = prescriptionRepository.findTodoPoolPrescriptions();
        if (todoList.isEmpty()) {
            throw new BusinessException("待办池为空，暂无可领取处方");
        }

        for (Prescription prescription : todoList) {
            try {
                return claimPrescription(prescription.getPrescriptionNo(), pharmacistId, pharmacistName);
            } catch (InvalidStatusException e) {
                continue;
            }
        }

        throw new BusinessException("暂无可领取处方");
    }

    @Transactional
    public PharmacistReviewDetailDTO submitReview(PharmacistReviewSubmitDTO dto) {
        log.info("药师[{}]提交处方[{}]审核结果: {}",
                dto.getPharmacistId(), dto.getPrescriptionNo(), dto.getConclusion());

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(dto.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + dto.getPrescriptionNo()));

        if (prescription.getStatus() != PrescriptionStatus.IN_PHARMACIST_REVIEW) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许提交审核结果", prescription.getStatus().getDescription()));
        }

        if (!dto.getPharmacistId().equals(prescription.getClaimedByPharmacistId())) {
            throw new BusinessException("该处方已被其他药师领取，您无法审核");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isTimeout = prescription.getReviewDeadline() != null
                && now.isAfter(prescription.getReviewDeadline());

        PharmacistReviewRecord reviewRecord = new PharmacistReviewRecord();
        reviewRecord.setPrescription(prescription);
        reviewRecord.setPharmacistId(dto.getPharmacistId());
        reviewRecord.setPharmacistName(dto.getPharmacistName() != null ? dto.getPharmacistName() : dto.getPharmacistId());
        reviewRecord.setConclusion(dto.getConclusion());
        reviewRecord.setReviewComments(dto.getReviewComments());
        reviewRecord.setReturnReason(dto.getReturnReason());
        reviewRecord.setAttentionReason(dto.getAttentionReason());
        reviewRecord.setClaimedAt(prescription.getClaimedAt());
        reviewRecord.setReviewedAt(now);
        reviewRecord.setReviewDurationSeconds(
                ChronoUnit.SECONDS.between(prescription.getClaimedAt(), now));
        reviewRecord.setIsTimeout(isTimeout);
        reviewRecordRepository.save(reviewRecord);

        switch (dto.getConclusion()) {
            case PASSED -> handlePassed(prescription, dto, now);
            case RETURNED_FOR_MODIFICATION -> handleReturned(prescription, dto, now);
            case KEY_ATTENTION -> handleKeyAttention(prescription, dto, now);
        }

        prescription.setReviewedByPharmacistId(dto.getPharmacistId());
        prescription.setReviewedByPharmacistName(
                dto.getPharmacistName() != null ? dto.getPharmacistName() : dto.getPharmacistId());
        prescription.setPharmacistReviewedAt(now);

        prescription = prescriptionRepository.save(prescription);

        if (isTimeout) {
            ReviewTimeoutEvent timeoutEvent = new ReviewTimeoutEvent();
            timeoutEvent.setPrescription(prescription);
            timeoutEvent.setPharmacistId(dto.getPharmacistId());
            timeoutEvent.setPharmacistName(dto.getPharmacistName());
            timeoutEvent.setTimeoutMinutes(
                    (int) ChronoUnit.MINUTES.between(prescription.getReviewDeadline(), now));
            timeoutEvent.setClaimedAt(prescription.getClaimedAt());
            timeoutEvent.setTimeoutAt(now);
            timeoutEventRepository.save(timeoutEvent);
        }

        return getReviewDetail(dto.getPrescriptionNo());
    }

    private void handlePassed(Prescription prescription, PharmacistReviewSubmitDTO dto, LocalDateTime now) {
        prescription.setStatus(PrescriptionStatus.PHARMACIST_REVIEW_PASSED);
        prescription.setPharmacistReviewComments(dto.getReviewComments());
        prescription.setIsKeyAttention(false);
        log.info("处方[{}]药师审核通过", prescription.getPrescriptionNo());

        boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
        if (preoccupySuccess) {
            prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
            log.info("处方[{}]药师审核通过且库存预占成功", prescription.getPrescriptionNo());

            try {
                dispenseQueueService.enqueue(prescription.getPrescriptionNo());
            } catch (Exception e) {
                log.warn("处方[{}]自动加入配药队列失败: {}", prescription.getPrescriptionNo(), e.getMessage());
            }
        } else {
            prescription.setStatus(PrescriptionStatus.PREOCCUPY_FAILED);
            log.info("处方[{}]药师审核通过但库存预占失败", prescription.getPrescriptionNo());
        }
    }

    private void handleReturned(Prescription prescription, PharmacistReviewSubmitDTO dto, LocalDateTime now) {
        prescription.setStatus(PrescriptionStatus.PENDING_MODIFICATION);
        prescription.setPharmacistReturnReason(dto.getReturnReason());
        prescription.setPharmacistReviewComments(dto.getReviewComments());
        log.info("处方[{}]药师退回修改，原因: {}", prescription.getPrescriptionNo(), dto.getReturnReason());
    }

    private void handleKeyAttention(Prescription prescription, PharmacistReviewSubmitDTO dto, LocalDateTime now) {
        prescription.setStatus(PrescriptionStatus.KEY_ATTENTION);
        prescription.setIsKeyAttention(true);
        prescription.setPharmacistAttentionReason(dto.getAttentionReason());
        prescription.setPharmacistReviewComments(dto.getReviewComments());
        log.info("处方[{}]标记为重点关注，原因: {}", prescription.getPrescriptionNo(), dto.getAttentionReason());

        boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
        if (preoccupySuccess) {
            prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
            log.info("处方[{}]重点关注通过且库存预占成功", prescription.getPrescriptionNo());

            try {
                dispenseQueueService.enqueue(prescription.getPrescriptionNo());
            } catch (Exception e) {
                log.warn("处方[{}]自动加入配药队列失败: {}", prescription.getPrescriptionNo(), e.getMessage());
            }
        } else {
            prescription.setStatus(PrescriptionStatus.PREOCCUPY_FAILED);
            log.info("处方[{}]重点关注通过但库存预占失败", prescription.getPrescriptionNo());
        }
    }

    @Transactional
    public ReviewTodoItemDTO releasePrescription(String prescriptionNo, String pharmacistId) {
        log.info("药师[{}]主动释放处方[{}]", pharmacistId, prescriptionNo);

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));

        if (prescription.getStatus() != PrescriptionStatus.IN_PHARMACIST_REVIEW) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许释放", prescription.getStatus().getDescription()));
        }

        if (!pharmacistId.equals(prescription.getClaimedByPharmacistId())) {
            throw new BusinessException("该处方不是您领取的，无法释放");
        }

        prescription.setStatus(PrescriptionStatus.PENDING_PHARMACIST_REVIEW);
        prescription.setClaimedByPharmacistId(null);
        prescription.setClaimedByPharmacistName(null);
        prescription.setClaimedAt(null);
        prescription.setReviewDeadline(null);

        prescription = prescriptionRepository.save(prescription);
        log.info("处方[{}]已释放回待办池", prescriptionNo);

        return convertToTodoItem(prescription);
    }

    public PharmacistReviewDetailDTO getReviewDetail(String prescriptionNo) {
        Prescription prescription = prescriptionRepository.findByPrescriptionNo(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));

        PharmacistReviewDetailDTO detail = new PharmacistReviewDetailDTO();
        detail.setId(prescription.getId());
        detail.setPrescriptionNo(prescription.getPrescriptionNo());
        detail.setPatientId(prescription.getPatientId());
        detail.setPatientName(prescription.getPatientName());
        detail.setDiagnosisCode(prescription.getDiagnosisCode());
        detail.setDiagnosisName(prescription.getDiagnosisName());
        detail.setDoctorId(prescription.getDoctorId());
        detail.setDoctorName(prescription.getDoctorName());
        detail.setDepartment(prescription.getDepartment());
        detail.setType(prescription.getType());
        detail.setTypeDescription(prescription.getType().getDescription());
        detail.setStatus(prescription.getStatus());
        detail.setStatusDescription(prescription.getStatus().getDescription());
        detail.setClaimedByPharmacistId(prescription.getClaimedByPharmacistId());
        detail.setClaimedByPharmacistName(prescription.getClaimedByPharmacistName());
        detail.setClaimedAt(prescription.getClaimedAt());
        detail.setReviewDeadline(prescription.getReviewDeadline());
        detail.setRemainingSeconds(calculateRemainingSeconds(prescription));
        detail.setIsKeyAttention(prescription.getIsKeyAttention());
        detail.setPharmacistAttentionReason(prescription.getPharmacistAttentionReason());
        detail.setCreatedAt(prescription.getCreatedAt());
        detail.setReviewComments(prescription.getReviewComments());

        if (prescription.getItems() != null) {
            detail.setItems(prescription.getItems().stream()
                    .map(item -> {
                        com.pharmacy.dto.PrescriptionItemDTO itemDTO = new com.pharmacy.dto.PrescriptionItemDTO();
                        itemDTO.setDrugCode(item.getDrugCode());
                        itemDTO.setDrugName(item.getDrugName());
                        itemDTO.setSingleDose(item.getSingleDose());
                        itemDTO.setDoseUnit(item.getDoseUnit());
                        itemDTO.setUsage(item.getUsage());
                        itemDTO.setFrequency(item.getFrequency());
                        itemDTO.setDays(item.getDays());
                        itemDTO.setTotalQuantity(item.getTotalQuantity());
                        itemDTO.setDispensingNotes(item.getDispensingNotes());
                        return itemDTO;
                    })
                    .toList());
        }

        detail.setRecentDrugs(getPatientRecentDispensedDrugs(prescription.getPatientId()));
        detail.setIngredientOverlapWarning(checkIngredientOverlap(prescription));

        return detail;
    }

    public List<PatientRecentDrugDTO> getPatientRecentDispensedDrugs(String patientId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Prescription> dispensedPrescriptions =
                prescriptionRepository.findDispensedPrescriptionsForPatient(patientId, sevenDaysAgo);

        return dispensedPrescriptions.stream()
                .map(p -> {
                    PatientRecentDrugDTO dto = new PatientRecentDrugDTO();
                    dto.setPrescriptionNo(p.getPrescriptionNo());
                    dto.setDispensedAt(p.getDispensedAt());

                    List<RecentDrugItemDTO> drugItems = p.getItems().stream()
                            .map(item -> {
                                RecentDrugItemDTO itemDTO = new RecentDrugItemDTO();
                                itemDTO.setDrugCode(item.getDrugCode());
                                itemDTO.setDrugName(item.getDrugName());
                                itemDTO.setTotalQuantity(item.getTotalQuantity());
                                itemDTO.setUnit(item.getDoseUnit());

                                drugRepository.findByDrugCode(item.getDrugCode()).ifPresent(drug -> {
                                    itemDTO.setSpecification(drug.getSpecification());
                                    itemDTO.setIngredient(drug.getIngredient());
                                });

                                return itemDTO;
                            })
                            .toList();
                    dto.setDrugs(drugItems);
                    return dto;
                })
                .toList();
    }

    public IngredientOverlapWarningDTO checkIngredientOverlap(Prescription prescription) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Prescription> dispensedPrescriptions =
                prescriptionRepository.findDispensedPrescriptionsForPatient(
                        prescription.getPatientId(), sevenDaysAgo);

        Map<String, OverlapDetailDTO> overlapMap = new HashMap<>();
        Set<String> currentIngredients = new HashSet<>();
        Map<String, String> currentIngredientDrugMap = new HashMap<>();
        Map<String, String> currentIngredientDrugCodeMap = new HashMap<>();

        for (PrescriptionItem item : prescription.getItems()) {
            drugRepository.findByDrugCode(item.getDrugCode()).ifPresent(drug -> {
                if (drug.getIngredient() != null && !drug.getIngredient().isEmpty()) {
                    String[] ingredients = drug.getIngredient().split("[,、]");
                    for (String ingredient : ingredients) {
                        String trimmed = ingredient.trim();
                        if (!trimmed.isEmpty()) {
                            currentIngredients.add(trimmed);
                            currentIngredientDrugMap.put(trimmed, drug.getName());
                            currentIngredientDrugCodeMap.put(trimmed, drug.getDrugCode());
                        }
                    }
                }
            });
        }

        for (Prescription dispensedPrescription : dispensedPrescriptions) {
            for (PrescriptionItem item : dispensedPrescription.getItems()) {
                drugRepository.findByDrugCode(item.getDrugCode()).ifPresent(drug -> {
                    if (drug.getIngredient() != null && !drug.getIngredient().isEmpty()) {
                        String[] ingredients = drug.getIngredient().split("[,、]");
                        for (String ingredient : ingredients) {
                            String trimmed = ingredient.trim();
                            if (!trimmed.isEmpty() && currentIngredients.contains(trimmed)) {
                                OverlapDetailDTO detail = overlapMap.computeIfAbsent(trimmed, k -> {
                                    OverlapDetailDTO d = new OverlapDetailDTO();
                                    d.setIngredient(k);
                                    d.setCurrentDrugName(currentIngredientDrugMap.get(k));
                                    d.setCurrentDrugCode(currentIngredientDrugCodeMap.get(k));
                                    d.setRecentDrugNames(new ArrayList<>());
                                    d.setRecentPrescriptionNos(new ArrayList<>());
                                    return d;
                                });

                                if (!detail.getRecentDrugNames().contains(drug.getName())) {
                                    detail.getRecentDrugNames().add(drug.getName());
                                }
                                if (!detail.getRecentPrescriptionNos().contains(dispensedPrescription.getPrescriptionNo())) {
                                    detail.getRecentPrescriptionNos().add(dispensedPrescription.getPrescriptionNo());
                                }
                            }
                        }
                    }
                });
            }
        }

        IngredientOverlapWarningDTO warning = new IngredientOverlapWarningDTO();
        warning.setHasOverlap(!overlapMap.isEmpty());
        warning.setOverlapDetails(new ArrayList<>(overlapMap.values()));

        if (!overlapMap.isEmpty()) {
            warning.setWarningMessage(String.format("发现%d种成分与近7天已发药品存在重叠", overlapMap.size()));
        } else {
            warning.setWarningMessage("未发现成分重叠");
        }

        return warning;
    }

    private ReviewTodoItemDTO convertToTodoItem(Prescription prescription) {
        ReviewTodoItemDTO dto = new ReviewTodoItemDTO();
        dto.setId(prescription.getId());
        dto.setPrescriptionNo(prescription.getPrescriptionNo());
        dto.setPatientId(prescription.getPatientId());
        dto.setPatientName(prescription.getPatientName());
        dto.setDiagnosisName(prescription.getDiagnosisName());
        dto.setDoctorName(prescription.getDoctorName());
        dto.setDepartment(prescription.getDepartment());
        dto.setType(prescription.getType());
        dto.setTypeDescription(prescription.getType().getDescription());
        dto.setStatus(prescription.getStatus());
        dto.setStatusDescription(prescription.getStatus().getDescription());
        dto.setItemCount(prescription.getItems() != null ? prescription.getItems().size() : 0);
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setReviewDeadline(prescription.getReviewDeadline());
        dto.setRemainingSeconds(calculateRemainingSeconds(prescription));
        dto.setIsEmergency(prescription.getType() == PrescriptionType.EMERGENCY);
        dto.setHasWarning(hasAutoReviewWarning(prescription));
        return dto;
    }

    private boolean hasAutoReviewWarning(Prescription prescription) {
        return prescription.getReviewResult() == ReviewResultType.WARNING;
    }

    private Long calculateRemainingSeconds(Prescription prescription) {
        if (prescription.getReviewDeadline() == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(prescription.getReviewDeadline())) {
            return 0L;
        }
        return ChronoUnit.SECONDS.between(now, prescription.getReviewDeadline());
    }

    private LocalDateTime calculateReviewDeadline(PrescriptionType type) {
        int minutes = switch (type) {
            case EMERGENCY -> EMERGENCY_REVIEW_MINUTES;
            case NORMAL, NARCOTIC -> NORMAL_REVIEW_MINUTES;
        };
        return LocalDateTime.now().plusMinutes(minutes);
    }

    public void sendPrescriptionToTodoPool(Prescription prescription) {
        if (prescription.getStatus() == PrescriptionStatus.REVIEW_PASSED
                || prescription.getStatus() == PrescriptionStatus.REVIEW_WARNING) {
            prescription.setStatus(PrescriptionStatus.PENDING_PHARMACIST_REVIEW);
            log.info("处方[{}]进入审方待办池", prescription.getPrescriptionNo());
        }
    }

    @Transactional
    public List<Prescription> releaseTimeoutReviews() {
        LocalDateTime now = LocalDateTime.now();
        List<Prescription> timeoutPrescriptions = prescriptionRepository.findTimeoutReviewPrescriptions(now);
        List<Prescription> releasedPrescriptions = new ArrayList<>();

        for (Prescription prescription : timeoutPrescriptions) {
            try {
                Prescription lockedPrescription = prescriptionRepository
                        .findByPrescriptionNoWithLock(prescription.getPrescriptionNo())
                        .orElse(null);

                if (lockedPrescription == null
                        || lockedPrescription.getStatus() != PrescriptionStatus.IN_PHARMACIST_REVIEW
                        || lockedPrescription.getReviewDeadline() == null
                        || lockedPrescription.getReviewDeadline().isAfter(now)) {
                    continue;
                }

                ReviewTimeoutEvent timeoutEvent = new ReviewTimeoutEvent();
                timeoutEvent.setPrescription(lockedPrescription);
                timeoutEvent.setPharmacistId(lockedPrescription.getClaimedByPharmacistId());
                timeoutEvent.setPharmacistName(lockedPrescription.getClaimedByPharmacistName());
                timeoutEvent.setTimeoutMinutes(
                        (int) ChronoUnit.MINUTES.between(lockedPrescription.getReviewDeadline(), now));
                timeoutEvent.setClaimedAt(lockedPrescription.getClaimedAt());
                timeoutEvent.setTimeoutAt(now);
                timeoutEventRepository.save(timeoutEvent);

                lockedPrescription.setStatus(PrescriptionStatus.PENDING_PHARMACIST_REVIEW);
                lockedPrescription.setClaimedByPharmacistId(null);
                lockedPrescription.setClaimedByPharmacistName(null);
                lockedPrescription.setClaimedAt(null);
                lockedPrescription.setReviewDeadline(null);

                prescriptionRepository.save(lockedPrescription);
                releasedPrescriptions.add(lockedPrescription);

                log.warn("处方[{}]审方超时，已释放回待办池，原领取药师: {}",
                        lockedPrescription.getPrescriptionNo(),
                        lockedPrescription.getClaimedByPharmacistId());
            } catch (Exception e) {
                log.error("处理超时处方[{}]失败", prescription.getPrescriptionNo(), e);
            }
        }

        return releasedPrescriptions;
    }
}
