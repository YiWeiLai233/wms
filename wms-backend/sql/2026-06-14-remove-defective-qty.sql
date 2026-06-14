-- 移除 stock 表的 defective_qty 字段
-- 次品和报废库存改为通过仓库类型（warehouse_type）区分，统一使用 quantity 字段
ALTER TABLE stock DROP COLUMN defective_qty;
