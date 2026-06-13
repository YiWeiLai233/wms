-- 出库单保存确认出库时选择的快递公司
ALTER TABLE outbound_order
    ADD COLUMN express_company_id BIGINT DEFAULT NULL COMMENT '快递公司ID' AFTER tracking_no;

CREATE INDEX idx_outbound_express_company_id ON outbound_order (express_company_id);
