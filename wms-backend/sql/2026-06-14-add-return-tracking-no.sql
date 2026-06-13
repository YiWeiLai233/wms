-- 退货单增加客户退货快递单号
ALTER TABLE return_order ADD COLUMN tracking_no VARCHAR(100) DEFAULT NULL COMMENT '客户退货快递单号' AFTER reason;
