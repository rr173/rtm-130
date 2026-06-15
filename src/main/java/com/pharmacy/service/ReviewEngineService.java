package com.pharmacy.service;

import com.pharmacy.dto.ReviewResultDTO;
import com.pharmacy.dto.ReviewRuleResultDTO;
import com.pharmacy.entity.Contraindication;
import com.pharmacy.entity.Doctor;
import com.pharmacy.entity.Drug;
import com.pharmacy.entity.Prescription;
import com.pharmacy.entity.PrescriptionItem;
import com.pharmacy.entity.ReviewRecord;
import com.pharmacy.enums.ContraindicationLevel;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.enums.ReviewResultType;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.ContraindicationRepository;
import com.pharmacy.repository.DoctorRepository;
import com.pharmacy.repository.DrugRepository;
import com.pharmacy.repository.PrescriptionItemRepository;
import com.pharmacy.repository.PrescriptionRepository;
import com.pharmacy.repository.ReviewRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEngineService {

    private final DrugRepository drugRepository;
    private final DoctorRepository doctorRepository;
    private final ContraindicationRepository contraindicationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final ReviewRecordRepository reviewRecordRepository;

    private static final String RULE_OVERDOSE = "OVERDOSE";
    private static final String RULE_DUPLICATE = "DUPLICATE";
    private static final String RULE_CONTRAINDICATION = "CONTRAINDICATION";
    private static final String RULE_SPECIAL_PERMISSION = "SPECIAL_PERMISSION";

    @Transactional
    public ReviewResultDTO reviewPrescription(Prescription prescription) {
        log.info("开始审核处方: {}", prescription.getPrescriptionNo());

        List<ReviewRuleResultDTO> ruleResults = new ArrayList<>();

        ruleResults.add(checkSpecialDrugPermission(prescription));
        ruleResults.add(checkOverdose(prescription));
        ruleResults.add(checkDuplicateMedication(prescription));
        ruleResults.add(checkContraindication(prescription));

        ReviewResultDTO overallResult = determineOverallResult(ruleResults);

        saveReviewRecords(prescription, ruleResults);

        prescription.setReviewResult(overallResult.getOverallResult());
        prescription.setReviewComments(overallResult.getOverallMessage());

        log.info("处方[{}]审核完成，结果: {}", prescription.getPrescriptionNo(),
                overallResult.getOverallResult());

        return overallResult;
    }

    private ReviewRuleResultDTO checkSpecialDrugPermission(Prescription prescription) {
        ReviewRuleResultDTO result = new ReviewRuleResultDTO();
        result.setRuleCode(RULE_SPECIAL_PERMISSION);
        result.setRuleName("特殊药品权限检查");
        result.setResult(ReviewResultType.PASSED);

        PrescriptionType type = prescription.getType();
        if (type != PrescriptionType.NARCOTIC) {
            result.setMessage("非麻精类处方，跳过权限检查");
            return result;
        }

        Doctor doctor = doctorRepository.findByDoctorId(prescription.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "医生不存在: " + prescription.getDoctorId()));

        boolean hasPermission = doctor.canPrescribe(type);
        if (!hasPermission) {
            result.setResult(ReviewResultType.BLOCKED);
            result.setMessage(String.format("医生[%s]不具备麻精类处方开具权限", doctor.getName()));
            log.warn("处方[{}]特殊药品权限检查不通过: 医生[{}]无麻精处方权",
                    prescription.getPrescriptionNo(), doctor.getName());
        } else {
            result.setMessage(String.format("医生[%s]具备麻精类处方权限", doctor.getName()));
        }

        return result;
    }

    private ReviewRuleResultDTO checkOverdose(Prescription prescription) {
        ReviewRuleResultDTO result = new ReviewRuleResultDTO();
        result.setRuleCode(RULE_OVERDOSE);
        result.setRuleName("单品超量检查");
        result.setResult(ReviewResultType.PASSED);

        List<String> overdoseDrugs = new ArrayList<>();

        for (PrescriptionItem item : prescription.getItems()) {
            Drug drug = drugRepository.findByDrugCode(item.getDrugCode()).orElse(null);
            if (drug == null) {
                continue;
            }

            BigDecimal maxDose = drug.getMaxSingleDose();
            if (maxDose == null || maxDose.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal itemDose = item.getSingleDose();
            if (itemDose == null) {
                continue;
            }

            if (itemDose.compareTo(maxDose) > 0) {
                overdoseDrugs.add(String.format("%s(单次剂量%s%s > 极量%s%s)",
                        drug.getName(), itemDose,
                        Optional.ofNullable(item.getDoseUnit()).orElse(""),
                        maxDose,
                        Optional.ofNullable(drug.getMaxSingleDoseUnit()).orElse("")));
                result.setDrugCode(drug.getDrugCode());
                result.setDrugName(drug.getName());
            }
        }

        if (!overdoseDrugs.isEmpty()) {
            result.setResult(ReviewResultType.BLOCKED);
            result.setMessage("存在超剂量药品: " + String.join(", ", overdoseDrugs));
            log.warn("处方[{}]单品超量检查不通过: {}", prescription.getPrescriptionNo(), result.getMessage());
        } else {
            result.setMessage("所有药品单次剂量均在极量范围内");
        }

        return result;
    }

    private ReviewRuleResultDTO checkDuplicateMedication(Prescription prescription) {
        ReviewRuleResultDTO result = new ReviewRuleResultDTO();
        result.setRuleCode(RULE_DUPLICATE);
        result.setRuleName("重复用药检查");
        result.setResult(ReviewResultType.PASSED);

        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        List<String> warnings = new ArrayList<>();
        Set<String> currentIngredients = new HashSet<>();
        Map<String, String> ingredientToDrugMap = new HashMap<>();
        Set<String> warnedIngredients = new HashSet<>();

        for (PrescriptionItem item : prescription.getItems()) {
            Drug drug = drugRepository.findByDrugCode(item.getDrugCode()).orElse(null);
            if (drug == null || drug.getIngredient() == null) {
                continue;
            }

            String[] ingredients = drug.getIngredient().split("[,、]");
            for (String ingredient : ingredients) {
                String trimmed = ingredient.trim();
                if (!trimmed.isEmpty()) {
                    if (currentIngredients.contains(trimmed)) {
                        warnings.add(String.format("处方内重复成分[%s]: %s 与 %s",
                                trimmed, ingredientToDrugMap.get(trimmed), drug.getName()));
                    } else {
                        currentIngredients.add(trimmed);
                        ingredientToDrugMap.put(trimmed, drug.getName());
                    }

                    if (!warnedIngredients.contains(trimmed)) {
                        List<PrescriptionItem> recentItems = prescriptionItemRepository
                                .findRecentItemsForPatientAndIngredient(
                                        prescription.getPatientId(), trimmed, startTime);

                        if (!recentItems.isEmpty()) {
                            warnings.add(String.format("患者24小时内已有含[%s]成分的处方", trimmed));
                            result.setDrugCode(drug.getDrugCode());
                            result.setDrugName(drug.getName());
                            warnedIngredients.add(trimmed);
                        }
                    }
                }
            }
        }

        if (!warnings.isEmpty()) {
            result.setResult(ReviewResultType.WARNING);
            result.setMessage("重复用药警告: " + String.join("; ", warnings));
            log.warn("处方[{}]重复用药检查警告: {}", prescription.getPrescriptionNo(), result.getMessage());
        } else {
            result.setMessage("未发现重复用药");
        }

        return result;
    }

    private ReviewRuleResultDTO checkContraindication(Prescription prescription) {
        ReviewRuleResultDTO result = new ReviewRuleResultDTO();
        result.setRuleCode(RULE_CONTRAINDICATION);
        result.setRuleName("配伍禁忌检查");
        result.setResult(ReviewResultType.PASSED);

        List<String> drugCodes = prescription.getItems().stream()
                .map(PrescriptionItem::getDrugCode)
                .toList();

        if (drugCodes.size() < 2) {
            result.setMessage("单药处方，无需配伍禁忌检查");
            return result;
        }

        List<Contraindication> contraindications =
                contraindicationRepository.findAllContraindicationsWithin(drugCodes);

        if (!contraindications.isEmpty()) {
            List<String> conflicts = new ArrayList<>();

            for (Contraindication c : contraindications) {
                Drug drugA = drugRepository.findByDrugCode(c.getDrugACode()).orElse(null);
                Drug drugB = drugRepository.findByDrugCode(c.getDrugBCode()).orElse(null);

                String drugAName = drugA != null ? drugA.getName() : c.getDrugACode();
                String drugBName = drugB != null ? drugB.getName() : c.getDrugBCode();

                if (c.getLevel() == ContraindicationLevel.SEVERE) {
                    result.setResult(ReviewResultType.BLOCKED);
                } else if (c.getLevel() == ContraindicationLevel.MODERATE
                        && result.getResult() != ReviewResultType.BLOCKED) {
                    result.setResult(ReviewResultType.WARNING);
                } else if (result.getResult() == ReviewResultType.PASSED) {
                    result.setResult(ReviewResultType.WARNING);
                }

                conflicts.add(String.format("[%s]与[%s]: %s", drugAName, drugBName, c.getDescription()));

                result.setDrugCode(c.getDrugACode());
                result.setDrugName(drugAName);
                result.setRelatedDrugCode(c.getDrugBCode());
                result.setRelatedDrugName(drugBName);
            }

            result.setMessage("存在配伍禁忌: " + String.join("; ", conflicts));
            log.warn("处方[{}]配伍禁忌检查不通过: {}", prescription.getPrescriptionNo(), result.getMessage());
        } else {
            result.setMessage("未发现配伍禁忌");
        }

        return result;
    }

    private ReviewResultDTO determineOverallResult(List<ReviewRuleResultDTO> ruleResults) {
        ReviewResultType overall = ruleResults.stream()
                .map(ReviewRuleResultDTO::getResult)
                .max(Comparator.comparingInt(r -> switch (r) {
                    case BLOCKED -> 3;
                    case WARNING -> 2;
                    case PASSED -> 1;
                }))
                .orElse(ReviewResultType.PASSED);

        String overallMessage = switch (overall) {
            case BLOCKED -> "处方审核不通过，存在拦截项";
            case WARNING -> "处方存在警告项，需药师确认";
            case PASSED -> "处方审核通过";
        };

        return new ReviewResultDTO(overall, ruleResults, overallMessage);
    }

    private void saveReviewRecords(Prescription prescription, List<ReviewRuleResultDTO> ruleResults) {
        for (ReviewRuleResultDTO dto : ruleResults) {
            ReviewRecord record = new ReviewRecord();
            record.setRuleCode(dto.getRuleCode());
            record.setRuleName(dto.getRuleName());
            record.setResult(dto.getResult());
            record.setMessage(dto.getMessage());
            record.setDrugCode(dto.getDrugCode());
            record.setRelatedDrugCode(dto.getRelatedDrugCode());
            prescription.addReviewRecord(record);
        }
    }
}
