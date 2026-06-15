# 药房处方调配与库存管控服务

基于Spring Boot 3.x + MySQL 8.0构建的医院药房处方调配与库存管控系统。

## 功能特性

### 处方管理
- ✅ 电子处方接收（REST API）
- ✅ 合理性审核引擎
  - 单品超量检查
  - 重复用药检查（24小时内相同成分）
  - 配伍禁忌检查
  - 特殊药品权限检查
- ✅ 审核结果：通过 / 警告放行（需药师确认） / 拦截
- ✅ 库存预占（从可用库存扣减到预占池）
- ✅ 配药指导
- ✅ 发药确认（预占量和实际库存同步扣减）
- ✅ 处方取消（释放预占库存）
- ✅ 处方查询（按患者、按状态）

### 库存管理
- ✅ 药品目录维护
- ✅ 配伍禁忌表维护
- ✅ 医生处方权限表维护
- ✅ 入库操作
- ✅ 盘点修正
- ✅ 库存流水记录（入库/预占/发药/释放）
- ✅ 发药时库存一致性校验

### 统计查询
- ✅ 按患者查询处方历史
- ✅ 按状态查询处方列表
- ✅ 按药品查询库存流水
- ✅ 按日期统计发药量
- ✅ 药品发药排行榜

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.2.x | 后端框架 |
| Spring Data JPA | 3.2.x | ORM框架 |
| MySQL | 8.0 | 数据库 |
| Maven | 3.9.x | 构建工具 |
| Docker | - | 容器化部署 |

## 项目结构

```
rtm-130/
├── src/main/java/com/pharmacy/
│   ├── PharmacyApplication.java      # 主启动类
│   ├── config/
│   │   └── DataInitializer.java      # 数据初始化
│   ├── controller/                    # REST API控制层
│   │   ├── PrescriptionController.java
│   │   ├── InventoryController.java
│   │   └── StatisticsController.java
│   ├── service/                       # 业务服务层
│   │   ├── PrescriptionService.java   # 处方服务
│   │   ├── ReviewEngineService.java   # 审核引擎
│   │   ├── InventoryService.java      # 库存服务
│   │   └── QueryService.java          # 查询服务
│   ├── repository/                    # 数据访问层
│   ├── entity/                        # 数据库实体
│   ├── dto/                           # 数据传输对象
│   ├── enums/                         # 枚举类
│   └── exception/                     # 异常处理
├── src/main/resources/
│   └── application.yml                # 应用配置
├── Dockerfile                         # Docker镜像构建
├── docker-compose.yml                 # Docker Compose编排
├── pom.xml                            # Maven配置
└── API_DOC.md                         # API文档
```

## 快速启动

### 前置条件
- Docker 20+
- Docker Compose 2.0+

### 一键启动
```bash
# 启动MySQL和应用服务
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 停止服务
docker-compose down

# 停止并清理数据
docker-compose down -v
```

### 服务地址
- API接口: http://localhost:8080/api
- MySQL: localhost:3306
  - 数据库: pharmacy_db
  - 用户名: pharmacy
  - 密码: pharmacy123

### 健康检查
```bash
# 查看库存概览
curl http://localhost:8080/api/inventory/summary

# 查看预置药品列表
curl http://localhost:8080/api/inventory/drugs
```

## 预置数据

系统启动时自动初始化以下数据：

### 药品目录（12种）
| 编码 | 名称 | 类别 | 可用库存 |
|------|------|------|----------|
| DRUG001 | 阿莫西林胶囊 | 抗生素 | 100 |
| DRUG002 | 布洛芬缓释胶囊 | 普通 | 150 |
| DRUG003 | 奥美拉唑肠溶胶囊 | 消化系统 | 80 |
| DRUG004 | 硝苯地平缓释片 | 心血管 | 120 |
| DRUG005 | 盐酸氨溴索口服溶液 | 呼吸系统 | 90 |
| DRUG006 | 头孢克肟分散片 | 抗生素 | 70 |
| DRUG007 | 复方甘草片 | 呼吸系统 | 200 |
| DRUG008 | 维生素C片 | 普通 | 500 |
| DRUG009 | 盐酸吗啡注射液 | 麻醉药品 | 50 |
| DRUG010 | 地西泮片 | 精神药品 | 30 |
| DRUG011 | 红霉素软膏 | 外用 | 300 |
| DRUG012 | 氯雷他定片 | 普通 | 110 |

### 配伍禁忌（3对）
1. **DRUG001 ↔ DRUG006** (严重禁忌): 阿莫西林与头孢克肟同属β-内酰胺类抗生素
2. **DRUG009 ↔ DRUG010** (中度禁忌): 吗啡与地西泮联用可能增强中枢抑制
3. **DRUG002 ↔ DRUG004** (轻度禁忌): 布洛芬可能降低硝苯地平的降压效果

### 医生（2位）
| ID | 姓名 | 科室 | 处方权限 |
|----|------|------|----------|
| DOC001 | 张医生 | 内科 | 普通、急诊、麻精 |
| DOC002 | 李医生 | 外科 | 普通、急诊 |

## 业务流程

### 处方状态流转
```
PENDING_REVIEW (待审核)
    ├─→ REVIEW_PASSED (审核通过)
    │       ├─→ PREOCCUPIED (已预占待配药) → DISPENSED (已发药)
    │       └─→ PREOCCUPY_FAILED (预占失败) → [补库后重试]
    ├─→ REVIEW_WARNING (审核警告) → [药师确认]
    │       └─→ 同上
    └─→ REVIEW_BLOCKED (审核拦截) → [终止]

任何未发药状态 → CANCELLED (已取消)
```

### 正常处方流转示例
```bash
# 1. 提交处方
curl -X POST http://localhost:8080/api/prescriptions \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST001",
  "patientId": "PAT001",
  "patientName": "张三",
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

# 2. 发药确认
curl -X POST http://localhost:8080/api/prescriptions/dispense \
-H "Content-Type: application/json" \
-d '{
  "prescriptionNo": "TEST001",
  "dispensedBy": "王药师"
}'

# 3. 查询库存流水
curl http://localhost:8080/api/inventory/logs/prescription/TEST001
```

## 审核规则

### 1. 单品超量检查 (OVERDOSE)
- 比较处方中单品单次剂量与药品极量
- 超量则**拦截**

### 2. 重复用药检查 (DUPLICATE)
- 检查处方内是否有相同成分药品
- 检查患者24小时内是否已有相同成分处方
- 发现则**警告**

### 3. 配伍禁忌检查 (CONTRAINDICATION)
- 检查处方中药品两两组合是否存在配伍禁忌
- 严重禁忌→**拦截**，中/轻度禁忌→**警告**

### 4. 特殊药品权限检查 (SPECIAL_PERMISSION)
- 麻精类处方需由有相应权限的医生开具
- 无权限则**拦截**

## 关键业务规则

### 库存预占
- 审核通过后，从**可用库存**扣减到**预占池**
- 任一药品库存不足，整张处方预占失败
- 预占失败后可补库重试
- 处方取消时释放预占回可用库存

### 发药确认
- 校验预占数据与实际库存一致性
- 如不一致（中间被盘点修正），**明确报错**，不静默发药
- 发药时预占量和实际库存同步扣减

### 并发控制
- 使用数据库行级锁（`PESSIMISTIC_WRITE`）处理库存并发操作
- 使用`@Version`乐观锁处理处方状态并发更新

## API接口

### 处方管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /prescriptions | 提交处方 |
| POST | /prescriptions/pharmacist-confirm | 药师确认警告处方 |
| POST | /prescriptions/dispense | 发药确认 |
| POST | /prescriptions/cancel | 取消处方 |
| POST | /prescriptions/{no}/retry-preoccupy | 重试预占 |
| GET | /prescriptions/{no} | 查询处方详情 |
| GET | /prescriptions/patient/{patientId} | 按患者查询 |
| GET | /prescriptions/status/{status} | 按状态查询 |
| GET | /prescriptions | 处方列表（分页） |

### 库存管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /inventory/stock-in | 药品入库 |
| POST | /inventory/adjust | 盘点修正 |
| GET | /inventory/drugs | 查询所有药品库存 |
| GET | /inventory/drugs/{code} | 查询单个药品 |
| GET | /inventory/summary | 库存概览 |
| GET | /inventory/logs | 库存流水 |
| GET | /inventory/logs/type/{type} | 按类型查流水 |
| GET | /inventory/logs/prescription/{no} | 按处方查流水 |

### 统计
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /statistics/daily-dispense | 每日发药统计 |
| GET | /statistics/dispense-ranking | 药品发药排行 |

完整API文档请参考 [API_DOC.md](./API_DOC.md)

## 本地开发（可选）

### 本地环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8.0+

### 本地运行
```bash
# 1. 创建数据库
CREATE DATABASE pharmacy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 配置数据库连接（修改application.yml或设置环境变量）

# 3. 构建
mvn clean package -DskipTests

# 4. 运行
java -jar target/pharmacy-service.jar
```

## 测试用例

详见 [API_DOC.md](./API_DOC.md) 中的测试用例章节，包含：
- 正常处方流转
- 单品超量（拦截）
- 配伍禁忌（警告）
- 无麻精权限（拦截）
- 库存不足（预占失败）

## 设计亮点

1. **状态机驱动**: 处方有严格的状态流转约束，非法操作直接拒绝
2. **审核引擎**: 可插拔的规则设计，易于扩展新的审核规则
3. **库存隔离**: 可用库存/预占池分离，避免超卖
4. **并发安全**: 行级锁 + 乐观锁，保证并发操作数据一致性
5. **完整溯源**: 每笔库存变动都有流水记录，可追溯
6. **错误透明**: 发药时库存不一致明确报错，而非静默处理
7. **容器化**: Docker Compose一键启动，包含数据库和应用
