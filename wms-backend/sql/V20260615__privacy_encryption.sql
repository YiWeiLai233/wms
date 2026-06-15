-- ============================================================
-- Customer privacy field encryption
-- ============================================================

ALTER TABLE sales_order
    MODIFY COLUMN receiver_name TEXT DEFAULT NULL COMMENT 'Receiver name (encrypted)',
    MODIFY COLUMN receiver_phone TEXT DEFAULT NULL COMMENT 'Receiver phone (encrypted)',
    MODIFY COLUMN receiver_address TEXT DEFAULT NULL COMMENT 'Receiver address (encrypted)';

ALTER TABLE sales_order
    ADD COLUMN receiver_name_hash CHAR(64) DEFAULT NULL COMMENT 'Receiver name HMAC hash for exact lookup',
    ADD COLUMN receiver_phone_hash CHAR(64) DEFAULT NULL COMMENT 'Receiver phone HMAC hash for exact lookup';

CREATE INDEX idx_sales_order_receiver_name_hash ON sales_order(receiver_name_hash);
CREATE INDEX idx_sales_order_receiver_phone_hash ON sales_order(receiver_phone_hash);
