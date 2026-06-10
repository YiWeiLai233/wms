-- 修改货架表，将 area_id 改为 warehouse_id，直接关联仓库

-- 添加 warehouse_id 字段
ALTER TABLE warehouse_shelf
    ADD COLUMN warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID' AFTER id;

-- 如果有数据，需要从库区表迁移仓库ID到货架表
-- UPDATE warehouse_shelf ws
-- INNER JOIN warehouse_area wa ON ws.area_id = wa.id
-- SET ws.warehouse_id = wa.warehouse_id;

-- 删除 area_id 字段（如果有数据需要先迁移）
-- ALTER TABLE warehouse_shelf DROP COLUMN area_id;
