-- ============================================================
-- 库存预警模板表
-- ============================================================

CREATE TABLE IF NOT EXISTS stock_alert_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    low_stock_threshold INT NOT NULL DEFAULT 10 COMMENT '低库存阈值',
    out_of_stock_threshold INT NOT NULL DEFAULT 0 COMMENT '缺货阈值',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警模板表';

-- ============================================================
-- 商品表添加预警模板关联字段
-- ============================================================

ALTER TABLE product ADD COLUMN alert_template_id BIGINT DEFAULT NULL COMMENT '预警模板ID';
ALTER TABLE product ADD KEY idx_alert_template_id (alert_template_id);
