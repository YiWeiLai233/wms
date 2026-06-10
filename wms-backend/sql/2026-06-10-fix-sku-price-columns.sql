-- 修复 product_sku 表的价格字段
-- 将原有的 price 字段拆分为 cost_price 和 sale_price

-- 添加成本价字段
ALTER TABLE product_sku
    ADD COLUMN cost_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '成本价' AFTER spec;

-- 添加售价字段
ALTER TABLE product_sku
    ADD COLUMN sale_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '售价' AFTER cost_price;

-- 将原有 price 数据迁移到 sale_price
UPDATE product_sku SET sale_price = price WHERE price IS NOT NULL;

-- 删除旧的 price 字段（可选，建议先备份）
-- ALTER TABLE product_sku DROP COLUMN price;
