-- 出库单表添加快递相关字段
ALTER TABLE outbound_order
    ADD COLUMN tracking_no VARCHAR(50) DEFAULT NULL COMMENT '快递单号' AFTER remark,
    ADD COLUMN shipping_fee DECIMAL(10,2) DEFAULT NULL COMMENT '快递费用' AFTER tracking_no;

-- 创建快递查询记录表（可选，用于缓存快递100查询结果）
CREATE TABLE IF NOT EXISTS express_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    tracking_no VARCHAR(50) NOT NULL COMMENT '快递单号',
    carrier VARCHAR(30) DEFAULT NULL COMMENT '快递公司编码',
    carrier_name VARCHAR(50) DEFAULT NULL COMMENT '快递公司名称',
    status VARCHAR(30) DEFAULT NULL COMMENT '快递状态',
    tracks TEXT DEFAULT NULL COMMENT '物流轨迹JSON',
    queried_at DATETIME DEFAULT NULL COMMENT '查询时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_tracking_no (tracking_no),
    KEY idx_queried_at (queried_at)
) ENGINE=InnoDB COMMENT='快递查询缓存表';
