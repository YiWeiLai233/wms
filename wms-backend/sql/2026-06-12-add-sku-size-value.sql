-- Add explicit shoe/clothing size value to SKU records.
ALTER TABLE product_sku
    ADD COLUMN size_value VARCHAR(30) DEFAULT NULL COMMENT '码数' AFTER name;

CREATE INDEX idx_product_sku_size_value ON product_sku (size_value);
