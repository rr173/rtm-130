package com.pharmacy.config;

import com.pharmacy.entity.Contraindication;
import com.pharmacy.entity.Doctor;
import com.pharmacy.entity.Drug;
import com.pharmacy.enums.ContraindicationLevel;
import com.pharmacy.enums.DrugCategory;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.repository.ContraindicationRepository;
import com.pharmacy.repository.DoctorRepository;
import com.pharmacy.repository.DrugRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DrugRepository drugRepository;
    private final DoctorRepository doctorRepository;
    private final ContraindicationRepository contraindicationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("开始初始化基础数据...");
        initDrugs();
        initDoctors();
        initContraindications();
        log.info("基础数据初始化完成");
    }

    private void initDrugs() {
        if (drugRepository.count() > 0) {
            log.info("药品数据已存在，跳过初始化");
            return;
        }

        List<Drug> drugs = Arrays.asList(
            createDrug("DRUG001", "阿莫西林胶囊", "0.5g*24粒", "盒", DrugCategory.ANTIBIOTIC,
                    new BigDecimal("2.0"), "g", "阿莫西林", 100, 0),
            createDrug("DRUG002", "布洛芬缓释胶囊", "0.3g*20粒", "盒", DrugCategory.NORMAL,
                    new BigDecimal("0.6"), "g", "布洛芬", 150, 0),
            createDrug("DRUG003", "奥美拉唑肠溶胶囊", "20mg*14粒", "盒", DrugCategory.GASTROINTESTINAL,
                    new BigDecimal("40"), "mg", "奥美拉唑", 80, 0),
            createDrug("DRUG004", "硝苯地平缓释片", "10mg*30片", "盒", DrugCategory.CARDIOVASCULAR,
                    new BigDecimal("40"), "mg", "硝苯地平", 120, 0),
            createDrug("DRUG005", "盐酸氨溴索口服溶液", "100ml:0.6g", "瓶", DrugCategory.RESPIRATORY,
                    new BigDecimal("60"), "mg", "氨溴索", 90, 0),
            createDrug("DRUG006", "头孢克肟分散片", "0.1g*6片", "盒", DrugCategory.ANTIBIOTIC,
                    new BigDecimal("0.4"), "g", "头孢克肟", 70, 0),
            createDrug("DRUG007", "复方甘草片", "100片", "瓶", DrugCategory.RESPIRATORY,
                    new BigDecimal("9"), "片", "甘草浸膏粉、阿片粉、樟脑", 200, 0),
            createDrug("DRUG008", "维生素C片", "100mg*100片", "瓶", DrugCategory.NORMAL,
                    new BigDecimal("1000"), "mg", "维生素C", 500, 0),
            createDrug("DRUG009", "盐酸吗啡注射液", "10mg:1ml", "支", DrugCategory.NARCOTIC,
                    new BigDecimal("30"), "mg", "吗啡", 50, 0),
            createDrug("DRUG010", "地西泮片", "2.5mg*100片", "瓶", DrugCategory.PSYCHOTROPIC,
                    new BigDecimal("10"), "mg", "地西泮", 30, 0),
            createDrug("DRUG011", "红霉素软膏", "1%:10g", "支", DrugCategory.EXTERNAL,
                    new BigDecimal("0"), "g", "红霉素", 300, 0),
            createDrug("DRUG012", "氯雷他定片", "10mg*6片", "盒", DrugCategory.NORMAL,
                    new BigDecimal("20"), "mg", "氯雷他定", 110, 0)
        );

        drugRepository.saveAll(drugs);
        log.info("初始化药品数据完成，共{}种药品", drugs.size());
    }

    private Drug createDrug(String code, String name, String spec, String unit, DrugCategory category,
                            BigDecimal maxDose, String doseUnit, String ingredient,
                            int availableStock, int preoccupiedStock) {
        Drug drug = new Drug();
        drug.setDrugCode(code);
        drug.setName(name);
        drug.setSpecification(spec);
        drug.setUnit(unit);
        drug.setCategory(category);
        drug.setMaxSingleDose(maxDose);
        drug.setMaxSingleDoseUnit(doseUnit);
        drug.setIngredient(ingredient);
        drug.setAvailableStock(availableStock);
        drug.setPreoccupiedStock(preoccupiedStock);
        return drug;
    }

    private void initDoctors() {
        if (doctorRepository.count() > 0) {
            log.info("医生数据已存在，跳过初始化");
            return;
        }

        Doctor doctor1 = new Doctor();
        doctor1.setDoctorId("DOC001");
        doctor1.setName("张医生");
        doctor1.setDepartment("内科");
        doctor1.setAllowedPrescriptionTypes(Arrays.asList(
                PrescriptionType.NORMAL,
                PrescriptionType.EMERGENCY,
                PrescriptionType.NARCOTIC
        ));

        Doctor doctor2 = new Doctor();
        doctor2.setDoctorId("DOC002");
        doctor2.setName("李医生");
        doctor2.setDepartment("外科");
        doctor2.setAllowedPrescriptionTypes(Arrays.asList(
                PrescriptionType.NORMAL,
                PrescriptionType.EMERGENCY
        ));

        doctorRepository.saveAll(Arrays.asList(doctor1, doctor2));
        log.info("初始化医生数据完成，共{}位医生", 2);
    }

    private void initContraindications() {
        if (contraindicationRepository.count() > 0) {
            log.info("配伍禁忌数据已存在，跳过初始化");
            return;
        }

        List<Contraindication> contraindications = Arrays.asList(
            createContraindication("DRUG001", "DRUG006", ContraindicationLevel.SEVERE,
                    "阿莫西林与头孢克肟同属β-内酰胺类抗生素，联用存在重复用药风险，可能增加肾毒性"),
            createContraindication("DRUG009", "DRUG010", ContraindicationLevel.MODERATE,
                    "吗啡与地西泮联用可能增强中枢抑制作用，导致呼吸抑制、低血压等风险"),
            createContraindication("DRUG002", "DRUG004", ContraindicationLevel.MILD,
                    "布洛芬可能降低硝苯地平的降压效果，联用需监测血压")
        );

        contraindicationRepository.saveAll(contraindications);
        log.info("初始化配伍禁忌数据完成，共{}条禁忌", contraindications.size());
    }

    private Contraindication createContraindication(String drugA, String drugB,
                                                     ContraindicationLevel level, String description) {
        Contraindication c = new Contraindication();
        c.setDrugACode(drugA);
        c.setDrugBCode(drugB);
        c.setLevel(level);
        c.setDescription(description);
        return c;
    }
}
