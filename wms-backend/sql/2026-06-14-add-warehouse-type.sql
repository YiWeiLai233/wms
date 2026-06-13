-- 仓库表新增类型字段，区分普通仓/次品仓/报废仓
ALTER TABLE warehouse ADD COLUMN warehouse_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '仓库类型：NORMAL-普通仓 DEFECTIVE-次品仓 SCRAP-报废仓';
