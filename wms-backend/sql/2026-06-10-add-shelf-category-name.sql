-- 添加 warehouse_shelf 表的 category_name 字段
ALTER TABLE warehouse_shelf
    ADD COLUMN category_name VARCHAR(100) DEFAULT NULL COMMENT '商品分类' AFTER name;
