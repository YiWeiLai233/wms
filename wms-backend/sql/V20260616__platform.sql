-- ============================================================
-- 平台管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS platform (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '平台ID',
    name VARCHAR(100) NOT NULL COMMENT '平台名称',
    color VARCHAR(20) DEFAULT NULL COMMENT '展示颜色（十六进制）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name_deleted (name, deleted),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台管理表';

-- ============================================================
-- 订单表添加平台字段
-- ============================================================

ALTER TABLE sales_order ADD COLUMN platform_id BIGINT DEFAULT NULL COMMENT '平台ID';
ALTER TABLE sales_order ADD KEY idx_platform_id (platform_id);
