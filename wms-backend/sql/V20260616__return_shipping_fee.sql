-- 退货单添加快递费字段
ALTER TABLE return_order ADD COLUMN shipping_fee DECIMAL(10,2) DEFAULT NULL COMMENT '退货快递费' AFTER tracking_no;
