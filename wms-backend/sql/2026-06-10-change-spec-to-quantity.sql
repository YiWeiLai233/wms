-- 将 product_sku 表的 spec（规格属性）字段改为 quantity（数量）字段

-- 添加 quantity 字段
ALTER TABLE product_sku
    ADD COLUMN quantity INT DEFAULT 0 COMMENT '数量' AFTER name;

-- 删除旧的 spec 字段（可选，建议先备份）
-- ALTER TABLE product_sku DROP COLUMN spec;
