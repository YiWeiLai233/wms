-- 换货单表添加快递公司字段
ALTER TABLE exchange_order ADD COLUMN express_company_id BIGINT DEFAULT NULL COMMENT '快递公司ID' AFTER return_tracking_no;
