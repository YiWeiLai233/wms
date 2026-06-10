-- 确保库存相关表存在

-- 当前库存表
CREATE TABLE IF NOT EXISTS stock (
    id            BIGINT  PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
    sku_id        BIGINT  NOT NULL COMMENT 'SKU ID',
    warehouse_id  BIGINT  NOT NULL COMMENT '仓库ID',
    location_id   BIGINT  NOT NULL COMMENT '库位ID',
    quantity      INT     NOT NULL DEFAULT 0 COMMENT '可用数量',
    locked_qty    INT     NOT NULL DEFAULT 0 COMMENT '锁定数量',
    defective_qty INT     NOT NULL DEFAULT 0 COMMENT '次品数量',
    deleted       TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sku_location (sku_id, location_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB COMMENT='当前库存表';

-- 库存流水表
CREATE TABLE IF NOT EXISTS stock_log (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    biz_type       VARCHAR(30)  NOT NULL COMMENT '业务类型：INBOUND/OUTBOUND/RETURN/ADJUST/LOCK/RELEASE',
    biz_no         VARCHAR(50)  NOT NULL COMMENT '业务单号',
    sku_id         BIGINT       NOT NULL COMMENT 'SKU ID',
    warehouse_id   BIGINT       NOT NULL COMMENT '仓库ID',
    location_id    BIGINT       NOT NULL COMMENT '库位ID',
    quantity_before INT         NOT NULL COMMENT '变动前数量',
    quantity_change INT         NOT NULL COMMENT '变动数量（正数入库，负数出库）',
    quantity_after  INT         NOT NULL COMMENT '变动后数量',
    operator_id    BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    remark         VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_biz (biz_type, biz_no),
    KEY idx_sku_id (sku_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='库存流水表';
