package com.pharmacy.service;

import com.pharmacy.dto.consultation.*;
import com.pharmacy.entity.ConsultationOpinion;
import com.pharmacy.entity.ConsultationRecord;
import com.pharmacy.entity.Prescription;
import com.pharmacy.enums.*;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.InvalidStatusException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.ConsultationOpinionRepository;
import com.pharmacy.repository.ConsultationRecordRepository;
import com.pharmacy.repository.PrescriptionRepository;
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
public class ConsultationService {

    private final ConsultationRecordRepository consultationRecordRepository;
    private final ConsultationOpinionRepository consultationOpinionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final InventoryService inventoryService;
    private final DispenseQueueService dispenseQueueService;

    private static final int NORMAL_CONSULTATION_MINUTES = 20;
    private static final int EMERGENCY_CONSULTATION_MINUTES = 8;

    @Transactional
    public ConsultationDetailDTO initiateConsultation(ConsultationInitiateDTO dto) {
        log.info("药师[{}]发起处方[{}]协同会诊，原因: {}",
                dto.getPharmacistId(), dto.getPrescriptionNo(), dto.getReason());

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(dto.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + dto.getPrescriptionNo()));

        if (prescription.getStatus() != PrescriptionStatus.IN_PHARMACIST_REVIEW) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许发起会诊", prescription.getStatus().getDescription()));
        }

        if (!dto.getPharmacistId().equals(prescription.getClaimedByPharmacistId())) {
            throw new BusinessException("只有领取该处方的药师才能发起会诊");
        }

        if (consultationRecordRepository.existsByPrescriptionNoAndStatus(
                dto.getPrescriptionNo(), ConsultationStatus.IN_PROGRESS)) {
            throw new BusinessException("该处方已有进行中的会诊");
        }

        List<String> invitedIds = dto.getInvitedPharmacistIds().stream()
                .distinct()
                .collect(Collectors.toList());
        if (invitedIds.size() < 1 || invitedIds.size() > 3) {
            throw new BusinessException("邀请药师人数必须在1-3人之间");
        }
        if (invitedIds.contains(dto.getPharmacistId())) {
            throw new BusinessException("不能邀请自己参与会诊");
        }

        ConsultationRecord consultation = new ConsultationRecord();
        consultation.setPrescription(prescription);
        consultation.setPrescriptionNo(prescription.getPrescriptionNo());
        consultation.setInitiatorPharmacistId(dto.getPharmacistId());
        consultation.setInitiatorPharmacistName(
                dto.getPharmacistName() != null ? dto.getPharmacistName() : dto.getPharmacistId());
        consultation.setReason(dto.getReason());
        consultation.setStatus(ConsultationStatus.IN_PROGRESS);
        consultation.setStartedAt(LocalDateTime.now());

        ConsultationOpinion primaryOpinion = new ConsultationOpinion();
        primaryOpinion.setPharmacistId(dto.getPharmacistId());
        primaryOpinion.setPharmacistName(
                dto.getPharmacistName() != null ? dto.getPharmacistName() : dto.getPharmacistId());
        primaryOpinion.setIsPrimary(true);
        primaryOpinion.setDeadline(calculateOpinionDeadline(prescription.getType()));
        consultation.addOpinion(primaryOpinion);

        for (String invitedPharmacistId : invitedIds) {
            ConsultationOpinion invitedOpinion = new ConsultationOpinion();
            invitedOpinion.setPharmacistId(invitedPharmacistId);
            invitedOpinion.setPharmacistName(invitedPharmacistId);
            invitedOpinion.setIsPrimary(false);
            invitedOpinion.setDeadline(calculateOpinionDeadline(prescription.getType()));
            consultation.addOpinion(invitedOpinion);
        }

        consultation = consultationRecordRepository.save(consultation);

        prescription.setStatus(PrescriptionStatus.IN_CONSULTATION);
        prescriptionRepository.save(prescription);

        log.info("处方[{}]协同会诊已发起，会诊ID: {}，参与人数: {}",
                dto.getPrescriptionNo(), consultation.getId(), consultation.getOpinions().size());

        return getConsultationDetail(consultation.getId());
    }

    @Transactional
    public ConsultationDetailDTO submitOpinion(Long consultationId, ConsultationOpinionSubmitDTO dto) {
        log.info("药师[{}]提交会诊[{}]意见: {}", dto.getPharmacistId(), consultationId, dto.getOpinionType());

        ConsultationRecord consultation = consultationRecordRepository.findByIdWithLock(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("会诊不存在: " + consultationId));

        if (consultation.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new InvalidStatusException(
                    String.format("会诊状态[%s]不允许提交意见", consultation.getStatus().getDescription()));
        }

        ConsultationOpinion opinion = consultationOpinionRepository
                .findByConsultationIdAndPharmacistId(consultationId, dto.getPharmacistId())
                .orElseThrow(() -> new BusinessException("您未被邀请参与此会诊"));

        if (opinion.getSubmittedAt() != null || opinion.getIsAbstained()) {
            throw new BusinessException("您已提交过意见，不可修改");
        }

        if (dto.getOpinionType() == ConsultationOpinionType.RETURN
                && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            throw new BusinessException("建议退回时必须填写理由");
        }
        if (dto.getOpinionType() == ConsultationOpinionType.KEY_ATTENTION
                && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            throw new BusinessException("建议重点关注时必须填写关注点");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isTimeout = opinion.getDeadline() != null && now.isAfter(opinion.getDeadline());

        opinion.setOpinionType(dto.getOpinionType());
        opinion.setReason(dto.getReason());
        opinion.setSubmittedAt(now);
        opinion.setIsTimeout(isTimeout);
        consultationOpinionRepository.save(opinion);

        boolean allCompleted = checkAllOpinionsCompleted(consultationId);
        if (allCompleted) {
            concludeConsultation(consultation);
        }

        return getConsultationDetail(consultationId);
    }

    @Transactional
    public void processTimeoutOpinions() {
        log.info("开始处理超时的会诊意见");
        LocalDateTime now = LocalDateTime.now();
        List<ConsultationOpinion> timeoutOpinions = consultationOpinionRepository.findAllTimeoutOpinions(now);

        Set<Long> affectedConsultationIds = new HashSet<>();
        for (ConsultationOpinion opinion : timeoutOpinions) {
            try {
                opinion.setIsAbstained(true);
                opinion.setIsTimeout(true);
                opinion.setSubmittedAt(now);
                opinion.setReason("超时未提交，视为弃权");
                consultationOpinionRepository.save(opinion);
                affectedConsultationIds.add(opinion.getConsultation().getId());
                log.warn("会诊[{}]中药师[{}]意见超时，已标记为弃权",
                        opinion.getConsultation().getId(), opinion.getPharmacistId());
            } catch (Exception e) {
                log.error("处理会诊意见超时失败，意见ID: {}", opinion.getId(), e);
            }
        }

        for (Long consultationId : affectedConsultationIds) {
            try {
                ConsultationRecord consultation = consultationRecordRepository.findByIdWithLock(consultationId)
                        .orElse(null);
                if (consultation != null && consultation.getStatus() == ConsultationStatus.IN_PROGRESS) {
                    boolean allCompleted = checkAllOpinionsCompleted(consultationId);
                    if (allCompleted) {
                        concludeConsultation(consultation);
                    }
                }
            } catch (Exception e) {
                log.error("处理会诊结论失败，会诊ID: {}", consultationId, e);
            }
        }

        log.info("超时会诊意见处理完成，共处理{}条，影响{}个会诊", timeoutOpinions.size(), affectedConsultationIds.size());
    }

    private boolean checkAllOpinionsCompleted(Long consultationId) {
        long total = consultationOpinionRepository.countTotalOpinionsByConsultationId(consultationId);
        long completed = consultationOpinionRepository.countCompletedOpinionsByConsultationId(consultationId);
        return total > 0 && total == completed;
    }

    @Transactional
    protected void concludeConsultation(ConsultationRecord consultation) {
        log.info("开始汇总会诊[{}]结论", consultation.getId());

        List<ConsultationOpinion> opinions = consultationOpinionRepository
                .findByConsultationIdOrderByCreatedAtAsc(consultation.getId());

        ConsultationOpinion primaryOpinion = opinions.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsPrimary()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("会诊缺少主审药师意见"));

        List<ConsultationOpinion> validOpinions = opinions.stream()
                .filter(o -> o.getOpinionType() != null && !Boolean.TRUE.equals(o.getIsAbstained()))
                .collect(Collectors.toList());

        Map<ConsultationOpinionType, Integer> voteCount = new EnumMap<>(ConsultationOpinionType.class);
        for (ConsultationOpinion op : validOpinions) {
            voteCount.merge(op.getOpinionType(), 1, Integer::sum);
        }

        int approveCount = voteCount.getOrDefault(ConsultationOpinionType.APPROVE, 0);
        int returnCount = voteCount.getOrDefault(ConsultationOpinionType.RETURN, 0);
        int keyAttentionCount = voteCount.getOrDefault(ConsultationOpinionType.KEY_ATTENTION, 0);
        int totalValid = validOpinions.size();
        int majorityThreshold = totalValid / 2 + 1;

        ConsultationConclusion conclusion;
        StringBuilder summaryComments = new StringBuilder();

        if (returnCount >= majorityThreshold) {
            conclusion = ConsultationConclusion.RETURNED;
            summaryComments.append("多数建议退回");
        } else if (approveCount >= majorityThreshold) {
            conclusion = ConsultationConclusion.PASSED;
            summaryComments.append("多数同意放行");
        } else if (keyAttentionCount >= majorityThreshold) {
            conclusion = ConsultationConclusion.KEY_ATTENTION;
            summaryComments.append("多数建议重点关注");
        } else {
            ConsultationOpinionType primaryType = primaryOpinion.getOpinionType();
            if (primaryType == null) {
                conclusion = ConsultationConclusion.RETURNED;
                summaryComments.append("票数相同且主审未投票，默认退回");
            } else {
                conclusion = switch (primaryType) {
                    case APPROVE -> ConsultationConclusion.PASSED;
                    case RETURN -> ConsultationConclusion.RETURNED;
                    case KEY_ATTENTION -> ConsultationConclusion.KEY_ATTENTION;
                };
                summaryComments.append("票数相同，以主审意见为准：").append(primaryType.getDescription());
            }
        }

        summaryComments.append("。投票结果：同意").append(approveCount)
                .append("票，退回").append(returnCount)
                .append("票，重点关注").append(keyAttentionCount)
                .append("票，弃权").append(opinions.size() - totalValid).append("票。");

        LocalDateTime now = LocalDateTime.now();
        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setFinalConclusion(conclusion);
        consultation.setSummaryComments(summaryComments.toString());
        consultation.setCompletedAt(now);
        if (consultation.getStartedAt() != null) {
            consultation.setTotalDurationSeconds(
                    ChronoUnit.SECONDS.between(consultation.getStartedAt(), now));
        }
        consultationRecordRepository.save(consultation);

        updatePrescriptionAfterConsultation(consultation, conclusion);

        log.info("会诊[{}]结论已生成: {}", consultation.getId(), conclusion.getDescription());
    }

    private void updatePrescriptionAfterConsultation(ConsultationRecord consultation,
                                                      ConsultationConclusion conclusion) {
        Prescription prescription = prescriptionRepository
                .findByPrescriptionNoWithLock(consultation.getPrescriptionNo())
                .orElse(null);

        if (prescription == null) {
            log.warn("处方不存在，无法更新状态: {}", consultation.getPrescriptionNo());
            return;
        }

        ConsultationOpinion primaryOpinion = consultation.getOpinions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsPrimary()))
                .findFirst()
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        switch (conclusion) {
            case PASSED -> {
                prescription.setStatus(PrescriptionStatus.PHARMACIST_REVIEW_PASSED);
                prescription.setPharmacistReviewComments(consultation.getSummaryComments());
                prescription.setReviewedByPharmacistId(consultation.getInitiatorPharmacistId());
                prescription.setReviewedByPharmacistName(consultation.getInitiatorPharmacistName());
                prescription.setPharmacistReviewedAt(now);
                prescription.setIsKeyAttention(false);

                boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
                if (preoccupySuccess) {
                    prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
                    try {
                        dispenseQueueService.enqueue(prescription.getPrescriptionNo());
                    } catch (Exception e) {
                        log.warn("处方[{}]自动加入配药队列失败: {}", prescription.getPrescriptionNo(), e.getMessage());
                    }
                } else {
                    prescription.setStatus(PrescriptionStatus.PREOCCUPY_FAILED);
                }
            }
            case RETURNED -> {
                prescription.setStatus(PrescriptionStatus.PENDING_MODIFICATION);
                prescription.setPharmacistReviewComments(consultation.getSummaryComments());
                prescription.setReviewedByPharmacistId(consultation.getInitiatorPharmacistId());
                prescription.setReviewedByPharmacistName(consultation.getInitiatorPharmacistName());
                prescription.setPharmacistReviewedAt(now);

                String returnReason = consultation.getOpinions().stream()
                        .filter(o -> o.getOpinionType() == ConsultationOpinionType.RETURN
                                && o.getReason() != null && !o.getReason().isEmpty())
                        .map(o -> o.getPharmacistName() + ": " + o.getReason())
                        .collect(Collectors.joining("; "));
                prescription.setPharmacistReturnReason(returnReason);
            }
            case KEY_ATTENTION -> {
                prescription.setStatus(PrescriptionStatus.KEY_ATTENTION);
                prescription.setIsKeyAttention(true);
                prescription.setPharmacistReviewComments(consultation.getSummaryComments());
                prescription.setReviewedByPharmacistId(consultation.getInitiatorPharmacistId());
                prescription.setReviewedByPharmacistName(consultation.getInitiatorPharmacistName());
                prescription.setPharmacistReviewedAt(now);

                String attentionReason = consultation.getOpinions().stream()
                        .filter(o -> o.getOpinionType() == ConsultationOpinionType.KEY_ATTENTION
                                && o.getReason() != null && !o.getReason().isEmpty())
                        .map(o -> o.getPharmacistName() + ": " + o.getReason())
                        .collect(Collectors.joining("; "));
                prescription.setPharmacistAttentionReason(attentionReason);

                boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
                if (preoccupySuccess) {
                    prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
                    try {
                        dispenseQueueService.enqueue(prescription.getPrescriptionNo());
                    } catch (Exception e) {
                        log.warn("处方[{}]自动加入配药队列失败: {}", prescription.getPrescriptionNo(), e.getMessage());
                    }
                } else {
                    prescription.setStatus(PrescriptionStatus.PREOCCUPY_FAILED);
                }
            }
        }

        prescriptionRepository.save(prescription);
    }

    private LocalDateTime calculateOpinionDeadline(PrescriptionType type) {
        int minutes = switch (type) {
            case EMERGENCY -> EMERGENCY_CONSULTATION_MINUTES;
            case NORMAL, NARCOTIC -> NORMAL_CONSULTATION_MINUTES;
        };
        return LocalDateTime.now().plusMinutes(minutes);
    }

    public ConsultationDetailDTO getConsultationDetail(Long consultationId) {
        ConsultationRecord consultation = consultationRecordRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("会诊不存在: " + consultationId));
        return convertToDetailDTO(consultation);
    }

    public ConsultationDetailDTO getConsultationByPrescriptionNo(String prescriptionNo) {
        ConsultationRecord consultation = consultationRecordRepository.findByPrescriptionNo(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方暂无会诊记录: " + prescriptionNo));
        return convertToDetailDTO(consultation);
    }

    public List<ConsultationTodoItemDTO> getMyTodoList(String pharmacistId) {
        List<ConsultationOpinion> opinions = consultationOpinionRepository
                .findByPharmacistIdOrderByCreatedAtDesc(pharmacistId);

        return opinions.stream()
                .filter(o -> o.getSubmittedAt() == null && !Boolean.TRUE.equals(o.getIsAbstained()))
                .map(opinion -> {
                    ConsultationRecord consultation = opinion.getConsultation();
                    Prescription prescription = consultation.getPrescription();
                    return convertToTodoItemDTO(consultation, prescription, opinion);
                })
                .sorted(Comparator.comparing(ConsultationTodoItemDTO::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<PharmacistConsultationHistoryDTO> getMyConsultationHistory(String pharmacistId) {
        List<ConsultationRecord> consultations = consultationRecordRepository
                .findByParticipatingPharmacistId(pharmacistId);

        return consultations.stream()
                .map(c -> convertToHistoryDTO(c, pharmacistId))
                .collect(Collectors.toList());
    }

    public List<ConsultationStatisticsDTO> getStatisticsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(23, 59, 59);

        List<ConsultationRecord> consultations = consultationRecordRepository
                .findByDateRange(startTime, endTime);

        Map<LocalDate, List<ConsultationRecord>> groupedByDate = consultations.stream()
                .filter(c -> c.getStatus() == ConsultationStatus.COMPLETED)
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate()));

        List<ConsultationStatisticsDTO> result = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            ConsultationStatisticsDTO dto = new ConsultationStatisticsDTO();
            dto.setDate(date);

            List<ConsultationRecord> dayConsultations = groupedByDate.getOrDefault(date, Collections.emptyList());
            dto.setConsultationCount(dayConsultations.size());

            if (!dayConsultations.isEmpty()) {
                double avgDuration = dayConsultations.stream()
                        .filter(c -> c.getTotalDurationSeconds() != null)
                        .mapToLong(ConsultationRecord::getTotalDurationSeconds)
                        .average()
                        .orElse(0.0);
                dto.setAvgDurationMinutes(avgDuration / 60.0);
            } else {
                dto.setAvgDurationMinutes(0.0);
            }

            result.add(dto);
            date = date.plusDays(1);
        }

        return result;
    }

    private ConsultationDetailDTO convertToDetailDTO(ConsultationRecord consultation) {
        ConsultationDetailDTO dto = new ConsultationDetailDTO();
        dto.setId(consultation.getId());
        dto.setPrescriptionNo(consultation.getPrescriptionNo());
        dto.setInitiatorPharmacistId(consultation.getInitiatorPharmacistId());
        dto.setInitiatorPharmacistName(consultation.getInitiatorPharmacistName());
        dto.setReason(consultation.getReason());
        dto.setStatus(consultation.getStatus());
        dto.setStatusDescription(consultation.getStatus().getDescription());
        dto.setFinalConclusion(consultation.getFinalConclusion());
        dto.setFinalConclusionDescription(
                consultation.getFinalConclusion() != null
                        ? consultation.getFinalConclusion().getDescription() : null);
        dto.setSummaryComments(consultation.getSummaryComments());
        dto.setStartedAt(consultation.getStartedAt());
        dto.setCompletedAt(consultation.getCompletedAt());
        dto.setTotalDurationSeconds(consultation.getTotalDurationSeconds());
        dto.setCreatedAt(consultation.getCreatedAt());
        dto.setUpdatedAt(consultation.getUpdatedAt());

        Prescription prescription = consultation.getPrescription();
        if (prescription != null) {
            dto.setPatientId(prescription.getPatientId());
            dto.setPatientName(prescription.getPatientName());
            dto.setDiagnosisName(prescription.getDiagnosisName());
        }

        if (consultation.getOpinions() != null) {
            dto.setOpinions(consultation.getOpinions().stream()
                    .map(this::convertToOpinionDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ConsultationOpinionDTO convertToOpinionDTO(ConsultationOpinion opinion) {
        ConsultationOpinionDTO dto = new ConsultationOpinionDTO();
        dto.setId(opinion.getId());
        dto.setPharmacistId(opinion.getPharmacistId());
        dto.setPharmacistName(opinion.getPharmacistName());
        dto.setOpinionType(opinion.getOpinionType());
        dto.setOpinionTypeDescription(
                opinion.getOpinionType() != null
                        ? opinion.getOpinionType().getDescription() : null);
        dto.setReason(opinion.getReason());
        dto.setIsPrimary(opinion.getIsPrimary());
        dto.setDeadline(opinion.getDeadline());
        dto.setSubmittedAt(opinion.getSubmittedAt());
        dto.setIsTimeout(opinion.getIsTimeout());
        dto.setIsAbstained(opinion.getIsAbstained());
        dto.setCreatedAt(opinion.getCreatedAt());
        return dto;
    }

    private ConsultationTodoItemDTO convertToTodoItemDTO(ConsultationRecord consultation,
                                                          Prescription prescription,
                                                          ConsultationOpinion opinion) {
        ConsultationTodoItemDTO dto = new ConsultationTodoItemDTO();
        dto.setConsultationId(consultation.getId());
        dto.setPrescriptionNo(consultation.getPrescriptionNo());
        dto.setPatientId(prescription.getPatientId());
        dto.setPatientName(prescription.getPatientName());
        dto.setDiagnosisName(prescription.getDiagnosisName());
        dto.setInitiatorPharmacistName(consultation.getInitiatorPharmacistName());
        dto.setReason(consultation.getReason());
        dto.setPrescriptionType(prescription.getType());
        dto.setPrescriptionTypeDescription(prescription.getType().getDescription());
        dto.setStatus(consultation.getStatus());
        dto.setStatusDescription(consultation.getStatus().getDescription());
        dto.setDeadline(opinion.getDeadline());
        dto.setRemainingSeconds(calculateRemainingSeconds(opinion.getDeadline()));
        dto.setCreatedAt(consultation.getCreatedAt());
        return dto;
    }

    private PharmacistConsultationHistoryDTO convertToHistoryDTO(ConsultationRecord consultation,
                                                                  String pharmacistId) {
        PharmacistConsultationHistoryDTO dto = new PharmacistConsultationHistoryDTO();
        dto.setConsultationId(consultation.getId());
        dto.setPrescriptionNo(consultation.getPrescriptionNo());

        Prescription prescription = consultation.getPrescription();
        if (prescription != null) {
            dto.setPatientName(prescription.getPatientName());
            dto.setDiagnosisName(prescription.getDiagnosisName());
        }

        dto.setInitiatorPharmacistName(consultation.getInitiatorPharmacistName());
        dto.setReason(consultation.getReason());
        dto.setStatus(consultation.getStatus());
        dto.setStatusDescription(consultation.getStatus().getDescription());
        dto.setFinalConclusion(consultation.getFinalConclusion());
        dto.setFinalConclusionDescription(
                consultation.getFinalConclusion() != null
                        ? consultation.getFinalConclusion().getDescription() : null);

        ConsultationOpinion myOpinion = consultation.getOpinions().stream()
                .filter(o -> o.getPharmacistId().equals(pharmacistId))
                .findFirst()
                .orElse(null);
        if (myOpinion != null) {
            dto.setMyOpinion(myOpinion.getOpinionType());
            dto.setMyOpinionDescription(
                    myOpinion.getOpinionType() != null
                            ? myOpinion.getOpinionType().getDescription() : null);
            dto.setParticipatedAt(myOpinion.getCreatedAt());
        }

        dto.setIsInitiator(consultation.getInitiatorPharmacistId().equals(pharmacistId));
        dto.setCompletedAt(consultation.getCompletedAt());

        return dto;
    }

    private Long calculateRemainingSeconds(LocalDateTime deadline) {
        if (deadline == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(deadline)) {
            return 0L;
        }
        return ChronoUnit.SECONDS.between(now, deadline);
    }
}
