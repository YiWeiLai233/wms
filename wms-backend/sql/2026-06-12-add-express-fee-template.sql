-- ============================================================
-- 快递费用模板功能
-- ============================================================

-- 快递公司表
CREATE TABLE IF NOT EXISTS express_company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公司ID',
    name VARCHAR(100) NOT NULL COMMENT '公司名称',
    code VARCHAR(50) NOT NULL COMMENT '公司编码',
    contact VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB COMMENT='快递公司表';

-- 费用模板表
CREATE TABLE IF NOT EXISTS express_fee_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    company_id BIGINT NOT NULL COMMENT '快递公司ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认模板',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_company_id (company_id)
) ENGINE=InnoDB COMMENT='快递费用模板表';

-- 费用阶梯表
CREATE TABLE IF NOT EXISTS express_fee_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阶梯ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    min_weight DECIMAL(10,2) NOT NULL COMMENT '最小重量(kg)，含',
    max_weight DECIMAL(10,2) NOT NULL COMMENT '最大重量(kg)，不含',
    fee DECIMAL(10,2) NOT NULL COMMENT '费用(元)',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_template_id (template_id)
) ENGINE=InnoDB COMMENT='快递费用阶梯表';

-- 初始化示例数据（可选）
-- INSERT INTO express_company (name, code) VALUES
-- ('顺丰速运', 'shunfeng'),
-- ('中通快递', 'zhongtong'),
-- ('圆通速递', 'yuantong');
