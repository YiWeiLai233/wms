-- ============================================================
-- 库存预警配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS stock_alert_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存预警配置ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID，NULL表示所有仓库通用',
    low_stock_threshold INT NOT NULL DEFAULT 10 COMMENT '低库存阈值',
    out_of_stock_threshold INT NOT NULL DEFAULT 0 COMMENT '缺货阈值',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_by_name VARCHAR(50) DEFAULT NULL COMMENT '创建人名称',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    updated_by_name VARCHAR(50) DEFAULT NULL COMMENT '更新人名称',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sku_warehouse_deleted (sku_id, warehouse_id, deleted),
    KEY idx_sku_id (sku_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警配置表';
