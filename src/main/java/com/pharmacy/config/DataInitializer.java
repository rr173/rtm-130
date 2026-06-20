package com.pharmacy.config;

import com.pharmacy.entity.Contraindication;
import com.pharmacy.entity.DispensingWindow;
import com.pharmacy.entity.Doctor;
import com.pharmacy.entity.Drug;
import com.pharmacy.entity.DrugBatch;
import com.pharmacy.entity.MonitoringPoint;
import com.pharmacy.entity.Pharmacist;
import com.pharmacy.enums.BatchStatus;
import com.pharmacy.enums.ContraindicationLevel;
import com.pharmacy.enums.DispenseChannel;
import com.pharmacy.enums.DispensingWindowStatus;
import com.pharmacy.enums.DrugCategory;
import com.pharmacy.enums.MonitoringPointType;
import com.pharmacy.enums.PrescriptionType;
import com.pharmacy.repository.ContraindicationRepository;
import com.pharmacy.repository.DispensingWindowRepository;
import com.pharmacy.repository.DoctorRepository;
import com.pharmacy.repository.DrugBatchRepository;
import com.pharmacy.repository.DrugRepository;
import com.pharmacy.repository.MonitoringPointRepository;
import com.pharmacy.repository.PharmacistRepository;
import com.pharmacy.service.reviewrule.ReviewRuleConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DrugRepository drugRepository;
    private final DoctorRepository doctorRepository;
    private final ContraindicationRepository contraindicationRepository;
    private final DispensingWindowRepository dispensingWindowRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final PharmacistRepository pharmacistRepository;
    private final ReviewRuleConfigService reviewRuleConfigService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("开始初始化基础数据...");
        initDrugs();
        initColdChainDrugs();
        initDoctors();
        initPharmacists();
        initContraindications();
        initDispensingWindows();
        initMonitoringPoints();
        initNormalDrugBatches();
        initColdChainBatches();
        reviewRuleConfigService.initializeDefaultConfig();
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

        if (code.equals("DRUG001")) {
            drug.setSplittable(true);
            drug.setPackageUnit("盒");
            drug.setPackageQuantity(24);
            drug.setSplitUnit("粒");
        } else if (code.equals("DRUG002")) {
            drug.setSplittable(true);
            drug.setPackageUnit("盒");
            drug.setPackageQuantity(20);
            drug.setSplitUnit("粒");
        } else if (code.equals("DRUG003")) {
            drug.setSplittable(true);
            drug.setPackageUnit("盒");
            drug.setPackageQuantity(14);
            drug.setSplitUnit("粒");
        } else if (code.equals("DRUG004")) {
            drug.setSplittable(true);
            drug.setPackageUnit("盒");
            drug.setPackageQuantity(30);
            drug.setSplitUnit("片");
        } else if (code.equals("DRUG007")) {
            drug.setSplittable(true);
            drug.setPackageUnit("瓶");
            drug.setPackageQuantity(100);
            drug.setSplitUnit("片");
        } else if (code.equals("DRUG008")) {
            drug.setSplittable(true);
            drug.setPackageUnit("瓶");
            drug.setPackageQuantity(100);
            drug.setSplitUnit("片");
        } else if (code.equals("DRUG010")) {
            drug.setSplittable(true);
            drug.setPackageUnit("瓶");
            drug.setPackageQuantity(100);
            drug.setSplitUnit("片");
        } else if (code.equals("DRUG012")) {
            drug.setSplittable(true);
            drug.setPackageUnit("盒");
            drug.setPackageQuantity(6);
            drug.setSplitUnit("片");
        }

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

    private void initPharmacists() {
        if (pharmacistRepository.count() > 0) {
            log.info("药师数据已存在，跳过初始化");
            return;
        }

        Pharmacist pharmacist1 = new Pharmacist();
        pharmacist1.setPharmacistId("PHARM001");
        pharmacist1.setName("王药师");
        pharmacist1.setTitle("主管药师");
        pharmacist1.setDepartment("药房");
        pharmacist1.setActive(true);

        Pharmacist pharmacist2 = new Pharmacist();
        pharmacist2.setPharmacistId("PHARM002");
        pharmacist2.setName("刘药师");
        pharmacist2.setTitle("药师");
        pharmacist2.setDepartment("药房");
        pharmacist2.setActive(true);

        Pharmacist pharmacist3 = new Pharmacist();
        pharmacist3.setPharmacistId("PHARM003");
        pharmacist3.setName("陈药师");
        pharmacist3.setTitle("副主任药师");
        pharmacist3.setDepartment("药房");
        pharmacist3.setActive(true);

        pharmacistRepository.saveAll(Arrays.asList(pharmacist1, pharmacist2, pharmacist3));
        log.info("初始化药师数据完成，共{}位药师", 3);
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

    private void initDispensingWindows() {
        if (dispensingWindowRepository.count() > 0) {
            log.info("配药窗口数据已存在，跳过初始化");
            return;
        }

        List<DispensingWindow> windows = Arrays.asList(
                createWindow("W1", "1号配药窗口(快速)", DispenseChannel.FAST),
                createWindow("W2", "2号配药窗口(普通)", DispenseChannel.NORMAL),
                createWindow("W3", "3号配药窗口(综合)", DispenseChannel.BOTH)
        );

        dispensingWindowRepository.saveAll(windows);
        log.info("初始化配药窗口数据完成，共{}个窗口", windows.size());
        log.info("窗口配置: W1=快速通道, W2=普通通道, W3=双通道(支持跨道调度)");
    }

    private DispensingWindow createWindow(String windowNo, String windowName, DispenseChannel serviceChannel) {
        DispensingWindow window = new DispensingWindow();
        window.setWindowNo(windowNo);
        window.setWindowName(windowName);
        window.setStatus(DispensingWindowStatus.IDLE);
        window.setServiceChannel(serviceChannel);
        return window;
    }

    private void initColdChainDrugs() {
        if (drugRepository.existsByDrugCode("VAC001")) {
            log.info("冷链药品数据已存在，跳过初始化");
            return;
        }

        List<Drug> coldChainDrugs = Arrays.asList(
            createDrug("VAC001", "新冠灭活疫苗", "0.5ml/支", "支", DrugCategory.VACCINE,
                    new BigDecimal("0.5"), "ml", "灭活新型冠状病毒抗原", 500, 0),
            createDrug("VAC002", "流感疫苗", "0.5ml/支", "支", DrugCategory.VACCINE,
                    new BigDecimal("0.5"), "ml", "流感病毒血凝素抗原", 300, 0),
            createDrug("VAC003", "乙肝疫苗", "10μg/0.5ml", "支", DrugCategory.VACCINE,
                    new BigDecimal("0.5"), "ml", "重组乙型肝炎病毒表面抗原", 400, 0),
            createDrug("BIO001", "重组人干扰素α2b注射液", "300万IU/1ml", "支", DrugCategory.BIOLOGICAL,
                    new BigDecimal("1"), "ml", "重组人干扰素α2b", 200, 0),
            createDrug("BIO002", "注射用重组人白介素-2", "20万IU/瓶", "瓶", DrugCategory.BIOLOGICAL,
                    new BigDecimal("1"), "瓶", "重组人白介素-2", 150, 0),
            createDrug("INS001", "精蛋白生物合成人胰岛素注射液", "300IU/3ml", "支", DrugCategory.INSULIN,
                    new BigDecimal("300"), "IU", "低精蛋白锌胰岛素", 180, 0),
            createDrug("INS002", "门冬胰岛素注射液", "300IU/3ml", "支", DrugCategory.INSULIN,
                    new BigDecimal("300"), "IU", "门冬胰岛素", 160, 0),
            createDrug("NOR001", "复方氨基酸注射液", "250ml/瓶", "瓶", DrugCategory.NORMAL,
                    new BigDecimal("250"), "ml", "多种氨基酸", 250, 0)
        );

        drugRepository.saveAll(coldChainDrugs);
        log.info("初始化冷链药品数据完成，共{}种药品", coldChainDrugs.size());
    }

    private void initMonitoringPoints() {
        if (monitoringPointRepository.count() > 0) {
            log.info("监控点数据已存在，跳过初始化");
            return;
        }

        MonitoringPoint refrigerator2to8 = new MonitoringPoint();
        refrigerator2to8.setPointCode("MP-R001");
        refrigerator2to8.setPointName("1号冷藏柜(2-8℃)");
        refrigerator2to8.setPointType(MonitoringPointType.REFRIGERATOR);
        refrigerator2to8.setLocationDescription("药房一楼冷库区A1位置，专用于存放疫苗类药品");
        refrigerator2to8.setTempMin(new BigDecimal("2.00"));
        refrigerator2to8.setTempMax(new BigDecimal("8.00"));
        refrigerator2to8.setHumidityMin(new BigDecimal("35.00"));
        refrigerator2to8.setHumidityMax(new BigDecimal("75.00"));
        refrigerator2to8.setEnabled(true);
        refrigerator2to8.setBoundBatchNos(Arrays.asList("VAC-B-20250101", "VAC-B-20250215", "VAC-B-20250310"));

        MonitoringPoint coolCabinet10to20 = new MonitoringPoint();
        coolCabinet10to20.setPointCode("MP-C001");
        coolCabinet10to20.setPointName("1号阴凉柜(10-20℃)");
        coolCabinet10to20.setPointType(MonitoringPointType.COOL_CABINET);
        coolCabinet10to20.setLocationDescription("药房一楼阴凉区B2位置，用于存放生物制剂和胰岛素");
        coolCabinet10to20.setTempMin(new BigDecimal("10.00"));
        coolCabinet10to20.setTempMax(new BigDecimal("20.00"));
        coolCabinet10to20.setHumidityMin(new BigDecimal("40.00"));
        coolCabinet10to20.setHumidityMax(new BigDecimal("70.00"));
        coolCabinet10to20.setEnabled(true);
        coolCabinet10to20.setBoundBatchNos(Arrays.asList("BIO-B-20250120", "BIO-B-20250301", "INS-B-20250201"));

        MonitoringPoint normalArea = new MonitoringPoint();
        normalArea.setPointCode("MP-N001");
        normalArea.setPointName("常温存储区");
        normalArea.setPointType(MonitoringPointType.NORMAL_AREA);
        normalArea.setLocationDescription("药房二楼常规药品存储区，温度0-30℃");
        normalArea.setTempMin(new BigDecimal("0.00"));
        normalArea.setTempMax(new BigDecimal("30.00"));
        normalArea.setHumidityMin(new BigDecimal("30.00"));
        normalArea.setHumidityMax(new BigDecimal("80.00"));
        normalArea.setEnabled(true);
        normalArea.setBoundBatchNos(Arrays.asList("NOR-B-20250115"));

        List<MonitoringPoint> points = Arrays.asList(refrigerator2to8, coolCabinet10to20, normalArea);
        monitoringPointRepository.saveAll(points);
        log.info("初始化监控点数据完成，共{}个监控点", points.size());
        log.info("  - MP-R001: 2-8℃冷藏柜，绑定疫苗类批次");
        log.info("  - MP-C001: 10-20℃阴凉柜，绑定生物制剂和胰岛素批次");
        log.info("  - MP-N001: 常温存储区，0-30℃");
    }

    private void initNormalDrugBatches() {
        if (drugBatchRepository.count() > 12) {
            log.info("常规药品批次数据已存在，跳过初始化");
            return;
        }

        List<DrugBatch> normalBatches = Arrays.asList(
            createBatch("DRUG001", "阿莫西林胶囊", "B-20250101",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2027, 12, 31),
                    100, new BigDecimal("25.80")),
            createBatch("DRUG002", "布洛芬缓释胶囊", "B-20250215",
                    LocalDate.of(2025, 2, 15), LocalDate.of(2027, 8, 14),
                    150, new BigDecimal("18.50")),
            createBatch("DRUG003", "奥美拉唑肠溶胶囊", "B-20250310",
                    LocalDate.of(2025, 3, 10), LocalDate.of(2028, 3, 9),
                    80, new BigDecimal("45.20")),
            createBatch("DRUG004", "硝苯地平缓释片", "B-20250120",
                    LocalDate.of(2025, 1, 20), LocalDate.of(2027, 1, 19),
                    120, new BigDecimal("32.80")),
            createBatch("DRUG005", "盐酸氨溴索口服溶液", "B-20250301",
                    LocalDate.of(2025, 3, 1), LocalDate.of(2026, 8, 31),
                    90, new BigDecimal("28.60")),
            createBatch("DRUG006", "头孢克肟分散片", "B-20250201",
                    LocalDate.of(2025, 2, 1), LocalDate.of(2027, 1, 31),
                    70, new BigDecimal("56.00")),
            createBatch("DRUG007", "复方甘草片", "B-20250410",
                    LocalDate.of(2025, 4, 10), LocalDate.of(2027, 4, 9),
                    200, new BigDecimal("12.50")),
            createBatch("DRUG008", "维生素C片", "B-20250115",
                    LocalDate.of(2025, 1, 15), LocalDate.of(2026, 7, 14),
                    500, new BigDecimal("8.50")),
            createBatch("DRUG009", "盐酸吗啡注射液", "B-20250220",
                    LocalDate.of(2025, 2, 20), LocalDate.of(2027, 2, 19),
                    50, new BigDecimal("35.00")),
            createBatch("DRUG010", "地西泮片", "B-20250315",
                    LocalDate.of(2025, 3, 15), LocalDate.of(2028, 3, 14),
                    30, new BigDecimal("22.00")),
            createBatch("DRUG011", "红霉素软膏", "B-20250105",
                    LocalDate.of(2025, 1, 5), LocalDate.of(2027, 1, 4),
                    300, new BigDecimal("6.80")),
            createBatch("DRUG012", "氯雷他定片", "B-20250225",
                    LocalDate.of(2025, 2, 25), LocalDate.of(2028, 2, 24),
                    110, new BigDecimal("28.00"))
        );

        drugBatchRepository.saveAll(normalBatches);
        log.info("初始化常规药品批次数据完成，共{}个批次", normalBatches.size());
    }

    private void initColdChainBatches() {
        if (drugBatchRepository.count() > 20) {
            log.info("药品批次数据较多，跳过冷链批次初始化");
            return;
        }

        List<DrugBatch> coldChainBatches = Arrays.asList(
            createBatch("VAC001", "新冠灭活疫苗", "VAC-B-20250101",
                    LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31),
                    500, new BigDecimal("128.00")),
            createBatch("VAC002", "流感疫苗", "VAC-B-20250215",
                    LocalDate.of(2025, 2, 15), LocalDate.of(2026, 8, 14),
                    300, new BigDecimal("156.00")),
            createBatch("VAC003", "乙肝疫苗", "VAC-B-20250310",
                    LocalDate.of(2025, 3, 10), LocalDate.of(2028, 3, 9),
                    400, new BigDecimal("89.00")),
            createBatch("BIO001", "重组人干扰素α2b注射液", "BIO-B-20250120",
                    LocalDate.of(2025, 1, 20), LocalDate.of(2027, 1, 19),
                    200, new BigDecimal("298.00")),
            createBatch("BIO002", "注射用重组人白介素-2", "BIO-B-20250301",
                    LocalDate.of(2025, 3, 1), LocalDate.of(2026, 8, 31),
                    150, new BigDecimal("560.00")),
            createBatch("INS001", "精蛋白生物合成人胰岛素注射液", "INS-B-20250201",
                    LocalDate.of(2025, 2, 1), LocalDate.of(2027, 1, 31),
                    180, new BigDecimal("198.00")),
            createBatch("INS002", "门冬胰岛素注射液", "INS-B-20250410",
                    LocalDate.of(2025, 4, 10), LocalDate.of(2027, 4, 9),
                    160, new BigDecimal("245.00")),
            createBatch("NOR001", "复方氨基酸注射液", "NOR-B-20250115",
                    LocalDate.of(2025, 1, 15), LocalDate.of(2026, 7, 14),
                    250, new BigDecimal("78.00"))
        );

        drugBatchRepository.saveAll(coldChainBatches);
        log.info("初始化冷链药品批次数据完成，共{}个批次", coldChainBatches.size());
    }

    private DrugBatch createBatch(String drugCode, String drugName, String batchNo,
                                   LocalDate productionDate, LocalDate expiryDate,
                                   int quantity, BigDecimal purchasePrice) {
        DrugBatch batch = new DrugBatch();
        batch.setDrugCode(drugCode);
        batch.setDrugName(drugName);
        batch.setBatchNo(batchNo);
        batch.setProductionDate(productionDate);
        batch.setExpiryDate(expiryDate);
        batch.setTotalQuantity(quantity);
        batch.setAvailableQuantity(quantity);
        batch.setPreoccupiedQuantity(0);
        batch.setSplitQuantity(0);
        batch.setDispensedQuantity(0);
        batch.setStatus(BatchStatus.NORMAL);
        batch.setSplitLocked(false);
        batch.setPurchasePrice(purchasePrice);
        batch.setOperator("SYSTEM");
        return batch;
    }
}
