# 药房处方调配与库存管控服务 - API文档

## 项目概述

基于Spring Boot 3.x + MySQL 8.0 构建的医院药房处方调配与库存管控系统。

## 快速启动

```bash
# 一键启动
docker-compose up -d

# 停止并清理
docker-compose down -v
```

服务启动后访问: `http://localhost:8080/api`

## 预置数据

### 药品目录(12种药品)
- DRUG001 阿莫西林胶囊 (抗生素)
- DRUG002 布洛芬缓释胶囊 (普通)
- DRUG003 奥美拉唑肠溶胶囊 (消化)
- DRUG004 硝苯地平缓释片 (心血管)
- DRUG005 盐酸氨溴索口服溶液 (呼吸)
- DRUG006 头孢克肟分散片 (抗生素)
- DRUG007 复方甘草片 (呼吸)
- DRUG008 维生素C片 (普通)
- DRUG009 盐酸吗啡注射液 (麻醉药品)
- DRUG010 地西泮片 (精神药品)
- DRUG011 红霉素软膏 (外用)
- DRUG012 氯雷他定片 (普通)

### 配伍禁忌(3对)
- DRUG001 ↔ DRUG006: 严重禁忌 - β-内酰胺类重复用药
- DRUG009 ↔ DRUG010: 中度禁忌 - 中枢抑制增强
- DRUG002 ↔ DRUG004: 轻度禁忌 - 降压效果影响

### 医生(2位)
- DOC001 张医生 (内科) - 有麻精处方权
- DOC002 李医生 (外科) - 无麻精处方权

## API接口列表

所有接口前缀: `/api`

---

### 一、处方管理接口

#### 1. 提交处方

**POST** `/prescriptions`

提交电子处方，系统自动进行合理性审核和库存预占。

**请求体:**
```json
{
  "prescriptionNo": "RX20240615001",
  "patientId": "PAT001",
  "patientName": "张三",
  "diagnosisCode": "J00",
  "diagnosisName": "上呼吸道感染",
  "doctorId": "DOC001",
  "doctorName": "张医生",
  "department": "内科",
  "type": "NORMAL",
  "items": [
    {
      "drugCode": "DRUG001",
      "drugName": "阿莫西林胶囊",
      "singleDose": 0.5,
      "doseUnit": "g",
      "usage": "ORAL",
      "frequency": "tid",
      "days": 7,
      "totalQuantity": 2,
      "dispensingNotes": "饭后服用"
    }
  ]
}
```

**处方类型(type)
- `NORMAL` - 普通
- `EMERGENCY` - 急诊
- `NARCOTIC` - 麻精

**用法(usage)
- `ORAL` - 口服
- `INJECTION` - 注射
- `EXTERNAL` - 外用
- `INHALATION` - 吸入
- 等等...

**处方状态流转:**
1. `PENDING_REVIEW` → `REVIEW_PASSED` → `PREOCCUPIED` → `DISPENSED`
2. `PENDING_REVIEW` → `REVIEW_WARNING` → (药师确认) → `PREOCCUPIED` → `DISPENSED`
3. `PENDING_REVIEW` → `REVIEW_BLOCKED` (终止)
4. `PENDING_REVIEW` → `REVIEW_PASSED` → `PREOCCUPY_FAILED` → (补库后重试) → `PREOCCUPIED` → `DISPENSED`

---

#### 2. 药师确认(审核警告处方)

**POST** `/prescriptions/pharmacist-confirm`

```json
{
  "prescriptionNo": "RX20240615001",
  "confirmed": true,
  "pharmacistName": "王药师",
  "comments": "已知风险，确认放行"
}
```

---

#### 3. 发药确认

**POST** `/prescriptions/dispense`

```json
{
  "prescriptionNo": "RX20240615001",
  "dispensedBy": "李药师",
  "remark": "已核对药品"
}
```

> **重要**: 发药时会校验预占数据与实际库存一致性，如不一致（中间被盘点修正），会明确报错。

---

#### 4. 取消处方

**POST** `/prescriptions/cancel`

```json
{
  "prescriptionNo": "RX20240615001",
  "reason": "患者放弃治疗",
  "operator": "系统管理员"
}
```

---

#### 5. 重试库存预占(预占失败时)

**POST** `/prescriptions/{prescriptionNo}/retry-preoccupy`

---

#### 6. 查询处方详情

**GET** `/prescriptions/{prescriptionNo}`

---

#### 7. 按患者查询处方历史

**GET** `/prescriptions/patient/{patientId}`

---

#### 8. 按状态查询处方列表

**GET** `/prescriptions/status/{status}?page=0&size=20`

状态列表: `PENDING_REVIEW`, `REVIEW_PASSED`, `REVIEW_WARNING`, `REVIEW_BLOCKED`, `PREOCCUPIED`, `PREOCCUPY_FAILED`, `DISPENSED`, `CANCELLED`

---

#### 9. 查询所有处方(分页)

**GET** `/prescriptions?page=0&size=20`

---

### 二、库存管理接口

#### 1. 药品入库

**POST** `/inventory/stock-in`

```json
{
  "drugCode": "DRUG001",
  "quantity": 100,
  "remark": "采购入库",
  "operator": "库管员"
}
```

---

#### 2. 盘点修正

**POST** `/inventory/adjust`

```json
{
  "drugCode": "DRUG001",
  "actualStock": 150,
  "remark": "盘点修正，实际库存与系统不符",
  "operator": "盘点员"
}
```

---

#### 3. 查询所有药品库存

**GET** `/inventory/drugs`

---

#### 4. 查询单个药品库存

**GET** `/inventory/drugs/{drugCode}`

---

#### 5. 库存概览

**GET** `/inventory/summary`

返回库存汇总信息: 药品总数、可用库存、预占库存、低库存药品数、管控药品数等。

---

### 三、库存流水查询

#### 1. 查询库存流水

**GET** `/inventory/logs?drugCode=DRUG001&startDate=2024-01-01&endDate=2024-12-31`

流水类型:
- `STOCK_IN` - 入库
- `PREOCCUPY` - 预占
- `DISPENSE` - 发药扣减
- `RELEASE` - 释放预占
- `ADJUST` - 盘点修正

#### 2. 按类型查询流水

**GET** `/inventory/logs/type/{type}`

#### 3. 按处方查询流水

**GET** `/inventory/logs/prescription/{prescriptionNo}`

---

### 四、统计接口

#### 1. 每日发药统计

**GET** `/statistics/daily-dispense?startDate=2024-01-01&endDate=2024-12-31`

返回按日期和药品汇总的发药量。

#### 2. 药品发药排行

**GET** `/statistics/dispense-ranking?startDate=2024-01-01&endDate=2024-12-31&limit=10`

---

## 审核规则说明

### 1. 单品超量检查(OVERDOSE)
- 拦截: 单次剂量超出药品说明书极量

### 2. 重复用药检查(DUPLICATE)
- 警告: 处方内多个药品含有相同成分
- 警告: 同一患者24小时内已有相同成分处方

### 3. 配伍禁忌检查(CONTRAINDICATION)
- 严重禁忌: 拦截
- 中度/轻度禁忌: 警告

### 4. 特殊药品权限检查(SPECIAL_PERMISSION)
- 拦截: 麻精类处方由无权限医生开具

---

## 测试用例

### 场景1: 正常处方流转

```bash
# 1. 提交普通处方(审核通过+预占成功)
curl -X POST http://localhost:8080/api/prescriptions \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST001",
  "patientId": "PAT001",
  "patientName": "测试患者",
  "doctorId": "DOC001",
  "doctorName": "张医生",
  "department": "内科",
  "type": "NORMAL",
  "items": [
    {
      "drugCode": "DRUG001",
      "drugName": "阿莫西林胶囊",
      "singleDose": 0.5,
      "doseUnit": "g",
      "usage": "ORAL",
      "frequency": "tid",
      "days": 7,
      "totalQuantity": 2
    }
  ]
}'

# 2. 发药确认
curl -X POST http://localhost:8080/api/prescriptions/dispense \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST001",
  "dispensedBy": "测试药师"
}'

# 3. 查询处方状态(应为DISPENSED)
curl http://localhost:8080/api/prescriptions/TEST001
```

### 场景2: 单品超量(拦截)

```bash
curl -X POST http://localhost:8080/api/prescriptions \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST002",
  "patientId": "PAT002",
  "doctorId": "DOC001",
  "type": "NORMAL",
  "items": [
    {
      "drugCode": "DRUG001",
      "drugName": "阿莫西林胶囊",
      "singleDose": 3.0,
      "doseUnit": "g",
      "usage": "ORAL",
      "frequency": "tid",
      "days": 7,
      "totalQuantity": 5
    }
  ]
}'
# 预期: 返回REVIEW_BLOCKED状态
```

### 场景3: 配伍禁忌(警告)

```bash
curl -X POST http://localhost:8080/api/prescriptions \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST003",
  "patientId": "PAT003",
  "doctorId": "DOC001",
  "type": "NORMAL",
  "items": [
    {
      "drugCode": "DRUG001",
      "drugName": "阿莫西林胶囊",
      "singleDose": 0.5,
      "doseUnit": "g",
      "usage": "ORAL",
      "frequency": "tid",
      "days": 7,
      "totalQuantity": 2
    },
    {
      "drugCode": "DRUG006",
      "drugName": "头孢克肟分散片",
      "singleDose": 0.2,
      "doseUnit": "g",
      "usage": "ORAL",
      "frequency": "bid",
      "days": 7,
      "totalQuantity": 2
    }
  ]
}'
# 预期: 返回REVIEW_WARNING状态，需要药师确认
```

### 场景4: 无麻精权限(拦截)

```bash
curl -X POST http://localhost:8080/api/prescriptions \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST004",
  "patientId": "PAT004",
  "doctorId": "DOC002",
  "type": "NARCOTIC",
  "items": [
    {
      "drugCode": "DRUG009",
      "drugName": "盐酸吗啡注射液",
      "singleDose": 10,
      "doseUnit": "mg",
      "usage": "INJECTION",
      "frequency": "prn",
      "days": 1,
      "totalQuantity": 2
    }
  ]
}'
# 预期: 返回REVIEW_BLOCKED状态
```

### 场景5: 库存不足(预占失败)

```bash
# 先将阿莫西林库存调整为1
curl -X POST http://localhost:8080/api/inventory/adjust \
-H "Content-Type: application/json" \
-d '{
  "drugCode": "DRUG001",
  "actualStock": 1,
  "operator": "测试"
}'

# 提交需要2盒的处方
curl -X POST http://localhost:8080/api/prescriptions \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST005",
  "patientId": "PAT005",
  "doctorId": "DOC001",
  "type": "NORMAL",
  "items": [
    {
      "drugCode": "DRUG001",
      "drugName": "阿莫西林胶囊",
      "singleDose": 0.5,
      "doseUnit": "g",
      "usage": "ORAL",
      "frequency": "tid",
      "days": 7,
      "totalQuantity": 2
    }
  ]
}'
# 预期: 返回PREOCCUPY_FAILED状态，lackDrugDetails字段说明缺药
```
