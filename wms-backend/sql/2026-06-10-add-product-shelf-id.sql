-- 添加 product 表的 shelf_id 字段
ALTER TABLE product
    ADD COLUMN shelf_id BIGINT DEFAULT NULL COMMENT '货架ID' AFTER category_id;

-- 添加索引
ALTER TABLE product
    ADD KEY idx_shelf_id (shelf_id);
