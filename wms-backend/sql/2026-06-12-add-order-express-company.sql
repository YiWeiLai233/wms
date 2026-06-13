-- Add prefilled express company on sales orders.
ALTER TABLE sales_order
    ADD COLUMN express_company VARCHAR(50) NULL COMMENT '快递公司' AFTER platform_order_no;
