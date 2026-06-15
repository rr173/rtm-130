package com.pharmacy.service;

import com.pharmacy.dto.CancelPrescriptionDTO;
import com.pharmacy.dto.DispenseConfirmDTO;
import com.pharmacy.dto.PharmacistConfirmDTO;
import com.pharmacy.dto.PrescriptionDTO;
import com.pharmacy.dto.PrescriptionItemDTO;
import com.pharmacy.dto.PrescriptionSubmitDTO;
import com.pharmacy.dto.ReviewResultDTO;
import com.pharmacy.entity.Prescription;
import com.pharmacy.entity.PrescriptionItem;
import com.pharmacy.enums.PrescriptionStatus;
import com.pharmacy.enums.ReviewResultType;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.InvalidStatusException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.DoctorRepository;
import com.pharmacy.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final DoctorRepository doctorRepository;
    private final ReviewEngineService reviewEngineService;
    private final InventoryService inventoryService;

    @Transactional
    public PrescriptionDTO submitPrescription(PrescriptionSubmitDTO dto) {
        log.info("收到处方提交: {}", dto.getPrescriptionNo());

        if (prescriptionRepository.existsByPrescriptionNo(dto.getPrescriptionNo())) {
            throw new BusinessException("处方编号已存在: " + dto.getPrescriptionNo());
        }

        if (!doctorRepository.existsByDoctorId(dto.getDoctorId())) {
            throw new ResourceNotFoundException("医生不存在: " + dto.getDoctorId());
        }

        Prescription prescription = convertToEntity(dto);
        prescription.setStatus(PrescriptionStatus.PENDING_REVIEW);

        prescription = prescriptionRepository.save(prescription);
        log.info("处方[{}]已保存，状态: {}", prescription.getPrescriptionNo(), prescription.getStatus());

        ReviewResultDTO reviewResult = reviewEngineService.reviewPrescription(prescription);

        updateStatusAfterReview(prescription, reviewResult);

        if (prescription.getStatus() == PrescriptionStatus.REVIEW_PASSED) {
            boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
            if (preoccupySuccess) {
                prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
                log.info("处方[{}]审核通过且库存预占成功，状态: {}",
                        prescription.getPrescriptionNo(), PrescriptionStatus.PREOCCUPIED);
            } else {
                prescription.setStatus(PrescriptionStatus.PREOCCUPY_FAILED);
                log.info("处方[{}]审核通过但库存预占失败，状态: {}",
                        prescription.getPrescriptionNo(), PrescriptionStatus.PREOCCUPY_FAILED);
            }
        }

        prescription = prescriptionRepository.save(prescription);
        return PrescriptionDTO.fromEntity(prescription);
    }

    @Transactional
    public PrescriptionDTO pharmacistConfirm(PharmacistConfirmDTO dto) {
        log.info("药师处理处方审核结果: {}, 确认结果: {}", dto.getPrescriptionNo(), dto.getConfirmed());

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(dto.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + dto.getPrescriptionNo()));

        if (prescription.getStatus() != PrescriptionStatus.REVIEW_WARNING) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许药师确认操作",
                            prescription.getStatus().getDescription()));
        }

        if (Boolean.TRUE.equals(dto.getConfirmed())) {
            prescription.setStatus(PrescriptionStatus.REVIEW_PASSED);
            prescription.setReviewComments(String.format("药师[%s]确认放行: %s",
                    dto.getPharmacistName(),
                    dto.getComments() != null ? dto.getComments() : ""));

            boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
            if (preoccupySuccess) {
                prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
                log.info("处方[{}]药师确认放行且库存预占成功", prescription.getPrescriptionNo());
            } else {
                prescription.setStatus(PrescriptionStatus.PREOCCUPY_FAILED);
                log.info("处方[{}]药师确认放行但库存预占失败", prescription.getPrescriptionNo());
            }
        } else {
            prescription.setStatus(PrescriptionStatus.REVIEW_BLOCKED);
            prescription.setReviewComments(String.format("药师[%s]拒绝放行: %s",
                    dto.getPharmacistName(),
                    dto.getComments() != null ? dto.getComments() : ""));
            log.info("处方[{}]药师拒绝放行", prescription.getPrescriptionNo());
        }

        prescription = prescriptionRepository.save(prescription);
        return PrescriptionDTO.fromEntity(prescription);
    }

    @Transactional
    public PrescriptionDTO dispenseConfirm(DispenseConfirmDTO dto) {
        log.info("处方发药确认: {}", dto.getPrescriptionNo());

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(dto.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + dto.getPrescriptionNo()));

        if (prescription.getStatus() != PrescriptionStatus.PREOCCUPIED) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许发药操作，需要先完成审核和库存预占",
                            prescription.getStatus().getDescription()));
        }

        inventoryService.dispenseStock(prescription, dto.getDispensedBy());

        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setDispensedBy(dto.getDispensedBy());
        prescription.setDispensedAt(LocalDateTime.now());

        prescription = prescriptionRepository.save(prescription);
        log.info("处方[{}]发药完成，发药人: {}", prescription.getPrescriptionNo(), dto.getDispensedBy());

        return PrescriptionDTO.fromEntity(prescription);
    }

    @Transactional
    public PrescriptionDTO cancelPrescription(CancelPrescriptionDTO dto) {
        log.info("取消处方: {}, 原因: {}", dto.getPrescriptionNo(), dto.getReason());

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(dto.getPrescriptionNo())
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + dto.getPrescriptionNo()));

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw new InvalidStatusException("已发药处方不能取消");
        }

        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new InvalidStatusException("处方已取消");
        }

        if (prescription.getStatus() == PrescriptionStatus.PREOCCUPIED
                || prescription.getStatus() == PrescriptionStatus.PREOCCUPY_FAILED) {
            inventoryService.releasePreoccupyStock(prescription, dto.getReason(), dto.getOperator());
        }

        prescription.setStatus(PrescriptionStatus.CANCELLED);
        prescription.setCancelReason(dto.getReason());
        prescription.setCancelledAt(LocalDateTime.now());

        prescription = prescriptionRepository.save(prescription);
        log.info("处方[{}]已取消", prescription.getPrescriptionNo());

        return PrescriptionDTO.fromEntity(prescription);
    }

    @Transactional
    public PrescriptionDTO retryPreoccupy(String prescriptionNo) {
        log.info("重试处方[{}]库存预占", prescriptionNo);

        Prescription prescription = prescriptionRepository.findByPrescriptionNoWithLock(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));

        if (prescription.getStatus() != PrescriptionStatus.PREOCCUPY_FAILED) {
            throw new InvalidStatusException(
                    String.format("处方状态[%s]不允许重试预占",
                            prescription.getStatus().getDescription()));
        }

        boolean preoccupySuccess = inventoryService.preoccupyStock(prescription);
        if (preoccupySuccess) {
            prescription.setStatus(PrescriptionStatus.PREOCCUPIED);
            prescription.setLackDrugDetails(null);
            log.info("处方[{}]库存预占重试成功", prescriptionNo);
        } else {
            log.info("处方[{}]库存预占重试失败", prescriptionNo);
        }

        prescription = prescriptionRepository.save(prescription);
        return PrescriptionDTO.fromEntity(prescription);
    }

    public PrescriptionDTO getPrescription(String prescriptionNo) {
        Prescription prescription = prescriptionRepository.findByPrescriptionNo(prescriptionNo)
                .orElseThrow(() -> new ResourceNotFoundException("处方不存在: " + prescriptionNo));
        return PrescriptionDTO.fromEntity(prescription);
    }

    public List<PrescriptionDTO> getPrescriptionsByPatient(String patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return prescriptions.stream()
                .map(PrescriptionDTO::fromEntity)
                .toList();
    }

    public Page<PrescriptionDTO> getPrescriptionsByStatus(PrescriptionStatus status, Pageable pageable) {
        Page<Prescription> prescriptionPage = prescriptionRepository.findByStatus(status, pageable);
        return prescriptionPage.map(PrescriptionDTO::fromEntity);
    }

    public Page<PrescriptionDTO> getAllPrescriptions(Pageable pageable) {
        Page<Prescription> prescriptionPage = prescriptionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return prescriptionPage.map(PrescriptionDTO::fromEntity);
    }

    private Prescription convertToEntity(PrescriptionSubmitDTO dto) {
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(dto.getPrescriptionNo());
        prescription.setPatientId(dto.getPatientId());
        prescription.setPatientName(dto.getPatientName());
        prescription.setDiagnosisCode(dto.getDiagnosisCode());
        prescription.setDiagnosisName(dto.getDiagnosisName());
        prescription.setDoctorId(dto.getDoctorId());
        prescription.setDoctorName(dto.getDoctorName());
        prescription.setDepartment(dto.getDepartment());
        prescription.setType(dto.getType());

        for (PrescriptionItemDTO itemDTO : dto.getItems()) {
            PrescriptionItem item = new PrescriptionItem();
            item.setDrugCode(itemDTO.getDrugCode());
            item.setDrugName(itemDTO.getDrugName());
            item.setSingleDose(itemDTO.getSingleDose());
            item.setDoseUnit(itemDTO.getDoseUnit());
            item.setUsage(itemDTO.getUsage());
            item.setFrequency(itemDTO.getFrequency());
            item.setDays(itemDTO.getDays());
            item.setTotalQuantity(itemDTO.getTotalQuantity());
            item.setDispensingNotes(itemDTO.getDispensingNotes());
            prescription.addItem(item);
        }

        return prescription;
    }

    private void updateStatusAfterReview(Prescription prescription, ReviewResultDTO result) {
        if (result.isBlocked()) {
            prescription.setStatus(PrescriptionStatus.REVIEW_BLOCKED);
        } else if (result.isWarning()) {
            prescription.setStatus(PrescriptionStatus.REVIEW_WARNING);
        } else {
            prescription.setStatus(PrescriptionStatus.REVIEW_PASSED);
        }
        log.info("处方[{}]审核后状态更新为: {}", prescription.getPrescriptionNo(),
                prescription.getStatus().getDescription());
    }
}
