-- ============================================================
-- WMS 数据库初始化脚本（合并所有迁移）
-- ============================================================

CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wms;

-- ============================================================
-- 1. 用户权限相关
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(50)  DEFAULT NULL,
    phone       VARCHAR(20)  DEFAULT NULL,
    email       VARCHAR(100) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(50)  NOT NULL,
    role_name   VARCHAR(50)  NOT NULL,
    description VARCHAR(200) DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    parent_id       BIGINT       DEFAULT 0,
    type            TINYINT      NOT NULL DEFAULT 1,
    sort_order      INT          DEFAULT 0,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB;

-- ============================================================
-- 2. 平台
-- ============================================================

CREATE TABLE IF NOT EXISTS platform (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    color       VARCHAR(20)  DEFAULT NULL,
    enabled     TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- 3. 商品相关
-- ============================================================

CREATE TABLE IF NOT EXISTS product_category (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT       DEFAULT 0,
    sort_order  INT          DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    spu_code          VARCHAR(50)   NOT NULL,
    name              VARCHAR(200)  NOT NULL,
    category_id       BIGINT        DEFAULT NULL,
    shelf_id          BIGINT        DEFAULT NULL,
    alert_template_id BIGINT        DEFAULT NULL,
    brand             VARCHAR(100)  DEFAULT NULL,
    main_image        VARCHAR(500)  DEFAULT NULL,
    description       TEXT          DEFAULT NULL,
    price             DECIMAL(10,2) DEFAULT 0.00,
    status            TINYINT       NOT NULL DEFAULT 1,
    deleted           TINYINT       NOT NULL DEFAULT 0,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_spu_code (spu_code),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_sku (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    product_id    BIGINT        NOT NULL,
    sku_code      VARCHAR(50)   NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    size_value    VARCHAR(50)   DEFAULT NULL,
    quantity      INT           DEFAULT 0,
    cost_price    DECIMAL(10,2) DEFAULT 0.00,
    sale_price    DECIMAL(10,2) DEFAULT 0.00,
    weight        DECIMAL(10,3) DEFAULT NULL,
    volume        DECIMAL(10,3) DEFAULT NULL,
    image         VARCHAR(500)  DEFAULT NULL,
    status        TINYINT       NOT NULL DEFAULT 1,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_barcode (
    id       BIGINT      PRIMARY KEY AUTO_INCREMENT,
    sku_id   BIGINT      NOT NULL,
    barcode  VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_barcode (barcode),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB;

-- ============================================================
-- 4. 仓库相关
-- ============================================================

CREATE TABLE IF NOT EXISTS warehouse (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    address        VARCHAR(500) DEFAULT NULL,
    contact        VARCHAR(50)  DEFAULT NULL,
    phone          VARCHAR(20)  DEFAULT NULL,
    status         TINYINT      NOT NULL DEFAULT 1,
    warehouse_type VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    deleted        TINYINT      NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS warehouse_area (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT       NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    type         TINYINT      DEFAULT 1,
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    KEY idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS warehouse_shelf (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    warehouse_id  BIGINT       DEFAULT NULL,
    area_id       BIGINT       NOT NULL DEFAULT 0,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    category_name VARCHAR(100) DEFAULT NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB;

-- ============================================================
-- 5. 库存相关
-- ============================================================

CREATE TABLE IF NOT EXISTS stock (
    id            BIGINT  PRIMARY KEY AUTO_INCREMENT,
    sku_id        BIGINT  NOT NULL,
    warehouse_id  BIGINT  NOT NULL,
    quantity      INT     NOT NULL DEFAULT 0,
    locked_qty    INT     NOT NULL DEFAULT 0,
    deleted       TINYINT NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_warehouse (sku_id, warehouse_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_log (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    biz_type        VARCHAR(30)  NOT NULL,
    biz_no          VARCHAR(50)  NOT NULL,
    sku_id          BIGINT       NOT NULL,
    warehouse_id    BIGINT       NOT NULL,
    quantity_before  INT         NOT NULL,
    quantity_change  INT         NOT NULL,
    quantity_after   INT         NOT NULL,
    operator_id     BIGINT       DEFAULT NULL,
    operator_name   VARCHAR(50)  DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_biz (biz_type, biz_no),
    KEY idx_sku_id (sku_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_check (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    check_no      VARCHAR(50)  NOT NULL,
    warehouse_id  BIGINT       NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0,
    remark        VARCHAR(500) DEFAULT NULL,
    operator_id   BIGINT       DEFAULT NULL,
    operator_name VARCHAR(50)  DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_check_no (check_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_check_item (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_id       BIGINT NOT NULL,
    sku_id         BIGINT NOT NULL,
    system_qty     INT    NOT NULL,
    actual_qty     INT    DEFAULT NULL,
    diff_qty       INT    DEFAULT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_check_id (check_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_alert_config (
    id                   BIGINT      PRIMARY KEY AUTO_INCREMENT,
    sku_id               BIGINT      NOT NULL,
    warehouse_id         BIGINT      DEFAULT NULL,
    low_stock_threshold  INT         DEFAULT 10,
    out_of_stock_threshold INT       DEFAULT 0,
    enabled              TINYINT     NOT NULL DEFAULT 1,
    deleted              TINYINT     NOT NULL DEFAULT 0,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_alert_template (
    id                     BIGINT      PRIMARY KEY AUTO_INCREMENT,
    name                   VARCHAR(100) NOT NULL,
    low_stock_threshold    INT         DEFAULT 10,
    out_of_stock_threshold INT         DEFAULT 0,
    deleted                TINYINT     NOT NULL DEFAULT 0,
    created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- 6. 订单相关
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_order (
    id                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_no             VARCHAR(50)   NOT NULL,
    platform_order_no    VARCHAR(50)   DEFAULT NULL,
    platform_id          BIGINT        DEFAULT NULL,
    warehouse_id         BIGINT        DEFAULT NULL,
    express_company_id   BIGINT        DEFAULT NULL,
    receiver_name        TEXT          DEFAULT NULL,
    receiver_phone       TEXT          DEFAULT NULL,
    receiver_address     TEXT          DEFAULT NULL,
    receiver_name_hash   CHAR(64)      DEFAULT NULL,
    receiver_phone_hash  CHAR(64)      DEFAULT NULL,
    order_status         VARCHAR(30)   NOT NULL DEFAULT 'WAIT_PAY',
    total_amount         DECIMAL(12,2) DEFAULT 0.00,
    remark               VARCHAR(500)  DEFAULT NULL,
    paid_at              DATETIME      DEFAULT NULL,
    shipped_at           DATETIME      DEFAULT NULL,
    finished_at          DATETIME      DEFAULT NULL,
    deleted              TINYINT       NOT NULL DEFAULT 0,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_platform_order_no (platform_order_no),
    KEY idx_order_status (order_status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales_order_item (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT        NOT NULL,
    sku_id        BIGINT        NOT NULL,
    sku_code      VARCHAR(50)   NOT NULL,
    sku_name      VARCHAR(200)  NOT NULL,
    size_value    VARCHAR(50)   DEFAULT NULL,
    quantity      INT           NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    total_price   DECIMAL(12,2) NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_id (order_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB;

-- ============================================================
-- 7. 出库相关
-- ============================================================

CREATE TABLE IF NOT EXISTS outbound_order (
    id                 BIGINT       PRIMARY KEY AUTO_INCREMENT,
    outbound_no        VARCHAR(50)  NOT NULL,
    order_id           BIGINT       NOT NULL,
    order_no           VARCHAR(50)  NOT NULL,
    warehouse_id       BIGINT       NOT NULL,
    status             VARCHAR(30)  NOT NULL DEFAULT 'WAIT_PICKING',
    picker_id          BIGINT       DEFAULT NULL,
    picker_name        VARCHAR(50)  DEFAULT NULL,
    remark             VARCHAR(500) DEFAULT NULL,
    tracking_no        VARCHAR(50)  DEFAULT NULL,
    express_company_id BIGINT       DEFAULT NULL,
    shipping_fee       DECIMAL(10,2) DEFAULT NULL,
    shipped_at         DATETIME     DEFAULT NULL,
    deleted            TINYINT      NOT NULL DEFAULT 0,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_outbound_no (outbound_no),
    KEY idx_order_id (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS outbound_order_item (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    outbound_id    BIGINT       NOT NULL,
    sku_id         BIGINT       NOT NULL,
    sku_code       VARCHAR(50)  NOT NULL,
    sku_name       VARCHAR(200) NOT NULL,
    quantity       INT          NOT NULL,
    picked_qty     INT          DEFAULT 0,
    shelf_id       BIGINT       DEFAULT NULL,
    scanned        TINYINT      DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_outbound_id (outbound_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB;

-- ============================================================
-- 8. 退货相关
-- ============================================================

CREATE TABLE IF NOT EXISTS return_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    return_no       VARCHAR(50)  NOT NULL,
    order_id        BIGINT       NOT NULL,
    order_no        VARCHAR(50)  NOT NULL,
    warehouse_id    BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_CHECK',
    reason          VARCHAR(500) DEFAULT NULL,
    tracking_no     VARCHAR(50)  DEFAULT NULL,
    shipping_fee    DECIMAL(10,2) DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    operator_id     BIGINT       DEFAULT NULL,
    operator_name   VARCHAR(50)  DEFAULT NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_return_no (return_no),
    KEY idx_order_id (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS return_order_item (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    return_id      BIGINT       NOT NULL,
    sku_id         BIGINT       NOT NULL,
    sku_code       VARCHAR(50)  NOT NULL,
    sku_name       VARCHAR(200) NOT NULL,
    quantity       INT          NOT NULL,
    quality_status VARCHAR(30)  DEFAULT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_return_id (return_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB;

-- ============================================================
-- 9. 换货相关
-- ============================================================

CREATE TABLE IF NOT EXISTS exchange_order (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    exchange_no         VARCHAR(50)  NOT NULL,
    order_id            BIGINT       NOT NULL,
    order_no            VARCHAR(50)  NOT NULL,
    warehouse_id        BIGINT       NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_RETURN',
    reason              VARCHAR(500) DEFAULT NULL,
    responsible_party   VARCHAR(20)  DEFAULT 'SELLER',
    return_tracking_no  VARCHAR(50)  DEFAULT NULL,
    shipping_fee        DECIMAL(10,2) DEFAULT NULL,
    express_company_id  BIGINT       DEFAULT NULL,
    remark              VARCHAR(500) DEFAULT NULL,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exchange_no (exchange_no),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS exchange_order_item (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    exchange_id   BIGINT       NOT NULL,
    item_type     VARCHAR(20)  NOT NULL,
    sku_id        BIGINT       NOT NULL,
    sku_code      VARCHAR(50)  NOT NULL,
    sku_name      VARCHAR(200) NOT NULL,
    quantity      INT          NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_exchange_id (exchange_id)
) ENGINE=InnoDB;

-- ============================================================
-- 10. 快递相关
-- ============================================================

CREATE TABLE IF NOT EXISTS express_company (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  DEFAULT NULL,
    enabled     TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS express_fee_template (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    name              VARCHAR(100)  NOT NULL,
    company_id        BIGINT        DEFAULT NULL,
    template_type     VARCHAR(20)   DEFAULT 'STEP',
    first_weight      DECIMAL(10,2) DEFAULT NULL,
    first_fee         DECIMAL(10,2) DEFAULT NULL,
    additional_weight DECIMAL(10,2) DEFAULT NULL,
    additional_fee    DECIMAL(10,2) DEFAULT NULL,
    is_default        TINYINT       NOT NULL DEFAULT 0,
    deleted           TINYINT       NOT NULL DEFAULT 0,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS express_fee_step (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT        NOT NULL,
    min_weight  DECIMAL(10,2) NOT NULL,
    max_weight  DECIMAL(10,2) NOT NULL,
    fee         DECIMAL(10,2) NOT NULL,
    KEY idx_template_id (template_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS express_cache (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tracking_no VARCHAR(50)  NOT NULL,
    company_code VARCHAR(50) DEFAULT NULL,
    response    TEXT         DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tracking_no (tracking_no)
) ENGINE=InnoDB;

-- ============================================================
-- 11. AI 助手相关
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     LONGTEXT     DEFAULT NULL,
    file_path   VARCHAR(500) DEFAULT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT       NOT NULL,
    content     TEXT         NOT NULL,
    embedding   JSON         DEFAULT NULL,
    chunk_index INT          DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_document_id (document_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    message_id  BIGINT       DEFAULT NULL,
    tool_name   VARCHAR(100) NOT NULL,
    arguments   TEXT         DEFAULT NULL,
    result      TEXT         DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_pending_action (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    action_type VARCHAR(50)  NOT NULL,
    params      JSON         DEFAULT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB;

-- ============================================================
-- 12. 系统相关
-- ============================================================

CREATE TABLE IF NOT EXISTS file_record (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    file_name     VARCHAR(200) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    file_size     BIGINT       DEFAULT NULL,
    file_type     VARCHAR(50)  DEFAULT NULL,
    biz_type      VARCHAR(30)  DEFAULT NULL,
    biz_id        BIGINT       DEFAULT NULL,
    uploader_id   BIGINT       DEFAULT NULL,
    uploader_name VARCHAR(50)  DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operation_log (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT       DEFAULT NULL,
    user_name      VARCHAR(50)  DEFAULT NULL,
    module         VARCHAR(50)  NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    target_type    VARCHAR(50)  DEFAULT NULL,
    target_id      BIGINT       DEFAULT NULL,
    detail         TEXT         DEFAULT NULL,
    ip             VARCHAR(50)  DEFAULT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS backup_record (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    backup_type VARCHAR(20)  NOT NULL,
    file_name   VARCHAR(200) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_size   BIGINT       DEFAULT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    remark      VARCHAR(500) DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS backup_config (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    auto_backup_enabled TINYINT      DEFAULT 0,
    auto_backup_type    VARCHAR(20)  DEFAULT 'FULL',
    auto_backup_time    VARCHAR(100) DEFAULT '0 0 2 * * ?',
    backup_path         VARCHAR(500) DEFAULT NULL,
    remote_backup_enabled TINYINT    DEFAULT 0,
    remote_host         VARCHAR(200) DEFAULT NULL,
    remote_port         INT          DEFAULT 22,
    remote_username     VARCHAR(100) DEFAULT NULL,
    remote_password     VARCHAR(200) DEFAULT NULL,
    remote_path         VARCHAR(500) DEFAULT NULL,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- 13. 初始化数据
-- ============================================================

-- 角色
INSERT INTO sys_role (role_code, role_name, description) VALUES
('SUPER_ADMIN', '超级管理员', '全部权限'),
('WAREHOUSE_ADMIN', '仓库管理员', '入库、出库、退货、库存盘点'),
('OPERATOR', '仓库操作员', '扫码入库、扫码出库、库存查询'),
('VIEWER', '查询员', '只读查询');

-- 管理员（密码：123456）
INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'SUPER_ADMIN';

-- 权限
INSERT INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order) VALUES
('dashboard', '仪表盘', 0, 1, 1),
('ai', '智能助手', 0, 1, 2),
('warehouse', '仓库管理', 0, 1, 3),
('product', '商品管理', 0, 1, 4),
('stock', '库存管理', 0, 1, 5),
('order', '订单中心', 0, 1, 6),
('express', '快递管理', 0, 1, 7),
('system', '系统管理', 0, 1, 8)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order) VALUES
('ai.assistant', 'AI 助手', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'ai' AND deleted = 0 LIMIT 1) t), 1, 1),
('ai.knowledge', '知识库管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'ai' AND deleted = 0 LIMIT 1) t), 1, 2),
('warehouse.list', '仓库管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'warehouse' AND deleted = 0 LIMIT 1) t), 1, 1),
('warehouse.shelf', '货架管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'warehouse' AND deleted = 0 LIMIT 1) t), 1, 2),
('warehouse.special', '特殊仓库管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'warehouse' AND deleted = 0 LIMIT 1) t), 1, 3),
('product.list', '商品列表', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'product' AND deleted = 0 LIMIT 1) t), 1, 1),
('product.sku', 'SKU管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'product' AND deleted = 0 LIMIT 1) t), 1, 2),
('stock.query', '库存查询', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'stock' AND deleted = 0 LIMIT 1) t), 1, 1),
('stock.log', '库存流水', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'stock' AND deleted = 0 LIMIT 1) t), 1, 2),
('stock.check', '盘点管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'stock' AND deleted = 0 LIMIT 1) t), 1, 3),
('order.list', '订单管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'order' AND deleted = 0 LIMIT 1) t), 1, 1),
('outbound.list', '发货管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'order' AND deleted = 0 LIMIT 1) t), 1, 2),
('returns.list', '退货管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'order' AND deleted = 0 LIMIT 1) t), 1, 3),
('exchange.list', '换货管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'order' AND deleted = 0 LIMIT 1) t), 1, 4),
('express.query', '快递查询', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'express' AND deleted = 0 LIMIT 1) t), 1, 1),
('express.company', '快递公司管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'express' AND deleted = 0 LIMIT 1) t), 1, 2),
('express.template', '费用模板管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'express' AND deleted = 0 LIMIT 1) t), 1, 3),
('express.report', '快递费用统计', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'express' AND deleted = 0 LIMIT 1) t), 1, 4),
('system.users', '用户管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1) t), 1, 1),
('system.roles', '角色管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1) t), 1, 2),
('system.files', '文件管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1) t), 1, 3),
('system.logs', '操作日志', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1) t), 1, 4),
('system.platforms', '平台管理', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1) t), 1, 5),
('system.backup', '数据库备份', (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1) t), 1, 6)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND r.deleted = 0 AND p.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 默认备份配置
INSERT INTO backup_config (auto_backup_enabled, auto_backup_type, auto_backup_time) VALUES (0, 'FULL', '0 0 2 * * ?');
