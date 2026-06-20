-- =============================================
-- 处方协同会诊与多药师联合审方模块 - 数据库脚本
-- =============================================

-- 会诊记录表
CREATE TABLE IF NOT EXISTS consultation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    prescription_id BIGINT NOT NULL COMMENT '处方ID',
    prescription_no VARCHAR(50) NOT NULL COMMENT '处方编号',
    initiator_pharmacist_id VARCHAR(50) NOT NULL COMMENT '发起药师ID(主审)',
    initiator_pharmacist_name VARCHAR(100) COMMENT '发起药师姓名',
    reason VARCHAR(1000) NOT NULL COMMENT '会诊原因',
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '会诊状态:IN_PROGRESS-会诊中,COMPLETED-已完成',
    final_conclusion VARCHAR(30) COMMENT '最终结论:PASSED-通过,RETURNED-退回,KEY_ATTENTION-重点关注通过',
    summary_comments VARCHAR(2000) COMMENT '汇总意见',
    started_at DATETIME COMMENT '会诊开始时间',
    completed_at DATETIME COMMENT '会诊完成时间',
    total_duration_seconds BIGINT COMMENT '总耗时(秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_initiator_pharmacist_id (initiator_pharmacist_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会诊记录表';

-- 会诊意见表
CREATE TABLE IF NOT EXISTS consultation_opinion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    consultation_id BIGINT NOT NULL COMMENT '会诊记录ID',
    pharmacist_id VARCHAR(50) NOT NULL COMMENT '药师ID',
    pharmacist_name VARCHAR(100) COMMENT '药师姓名',
    opinion_type VARCHAR(30) COMMENT '意见类型:APPROVE-同意放行,RETURN-建议退回,KEY_ATTENTION-建议重点关注',
    reason VARCHAR(1000) COMMENT '理由/关注点',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主审药师',
    deadline DATETIME COMMENT '意见提交时限',
    submitted_at DATETIME COMMENT '提交时间',
    is_timeout TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否超时',
    is_abstained TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否弃权',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_consultation_id (consultation_id),
    INDEX idx_pharmacist_id (pharmacist_id),
    INDEX idx_deadline (deadline),
    INDEX idx_submitted_at (submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会诊意见表';

-- =============================================
-- 示例数据
-- =============================================

-- 示例：插入一条会诊记录
-- INSERT INTO consultation_record (
--     prescription_id, prescription_no, initiator_pharmacist_id, initiator_pharmacist_name,
--     reason, status, started_at
-- ) VALUES (
--     1, 'RX202501010001', 'pharmacist001', '张药师',
--     '患者有多种基础疾病，处方用药复杂，需多药师联合审核',
--     'IN_PROGRESS', NOW()
-- );

-- 示例：插入会诊意见
-- INSERT INTO consultation_opinion (
--     consultation_id, pharmacist_id, pharmacist_name, opinion_type, reason, is_primary, deadline
-- ) VALUES
-- (1, 'pharmacist001', '张药师', NULL, NULL, 1, DATE_ADD(NOW(), INTERVAL 20 MINUTE)),
-- (1, 'pharmacist002', '李药师', NULL, NULL, 0, DATE_ADD(NOW(), INTERVAL 20 MINUTE)),
-- (1, 'pharmacist003', '王药师', NULL, NULL, 0, DATE_ADD(NOW(), INTERVAL 20 MINUTE));
