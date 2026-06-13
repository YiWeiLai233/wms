-- ============================================================
-- 易仓 WMS 数据库建表脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wms;

-- ============================================================
-- 1. 用户权限相关
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    parent_id       BIGINT       DEFAULT 0 COMMENT '父权限ID',
    type            TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1-菜单 2-按钮',
    sort_order      INT          DEFAULT 0 COMMENT '排序',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id  BIGINT NOT NULL COMMENT '用户ID',
    role_id  BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- ============================================================
-- 2. 商品相关
-- ============================================================

-- 商品分类表
CREATE TABLE IF NOT EXISTS product_category (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父分类ID',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='商品分类表';

-- 商品表（SPU）
CREATE TABLE IF NOT EXISTS product (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    spu_code      VARCHAR(50)  NOT NULL COMMENT 'SPU编码',
    name          VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id   BIGINT       DEFAULT NULL COMMENT '分类ID',
    brand         VARCHAR(100) DEFAULT NULL COMMENT '品牌',
    main_image    VARCHAR(500) DEFAULT NULL COMMENT '主图URL',
    description   TEXT         DEFAULT NULL COMMENT '商品描述',
    price         DECIMAL(10,2) DEFAULT 0.00 COMMENT '参考售价',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-上架 0-下架',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_spu_code (spu_code),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB COMMENT='商品表（SPU）';

-- 商品SKU表
CREATE TABLE IF NOT EXISTS product_sku (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT 'SKU ID',
    product_id    BIGINT        NOT NULL COMMENT '商品ID（SPU）',
    sku_code      VARCHAR(50)   NOT NULL COMMENT 'SKU编码',
    name          VARCHAR(200)  NOT NULL COMMENT 'SKU名称',
    spec          VARCHAR(500)  DEFAULT NULL COMMENT '规格属性（JSON格式，如 {"颜色":"红","尺码":"XL"}）',
    price         DECIMAL(10,2) DEFAULT 0.00 COMMENT '单价',
    weight        DECIMAL(10,3) DEFAULT NULL COMMENT '重量（kg）',
    volume        DECIMAL(10,3) DEFAULT NULL COMMENT '体积（m³）',
    image         VARCHAR(500)  DEFAULT NULL COMMENT 'SKU图片',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB COMMENT='商品SKU表';

-- 商品条码表
CREATE TABLE IF NOT EXISTS product_barcode (
    id       BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    sku_id   BIGINT      NOT NULL COMMENT 'SKU ID',
    barcode  VARCHAR(50) NOT NULL COMMENT '条码',
    UNIQUE KEY uk_barcode (barcode),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB COMMENT='商品条码表';

-- ============================================================
-- 3. 仓库相关
-- ============================================================

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouse (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
    code        VARCHAR(50)  NOT NULL COMMENT '仓库编码',
    name        VARCHAR(100) NOT NULL COMMENT '仓库名称',
    address     VARCHAR(500) DEFAULT NULL COMMENT '仓库地址',
    contact     VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB COMMENT='仓库表';

-- 库区表
CREATE TABLE IF NOT EXISTS warehouse_area (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '库区ID',
    warehouse_id BIGINT       NOT NULL COMMENT '仓库ID',
    code         VARCHAR(50)  NOT NULL COMMENT '库区编码',
    name         VARCHAR(100) NOT NULL COMMENT '库区名称',
    type         TINYINT      DEFAULT 1 COMMENT '类型：1-普通区 2-退货区 3-次品区',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    KEY idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB COMMENT='库区表';

-- 货架表
CREATE TABLE IF NOT EXISTS warehouse_shelf (
    id       BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '货架ID',
    area_id  BIGINT       NOT NULL COMMENT '库区ID',
    code     VARCHAR(50)  NOT NULL COMMENT '货架编码',
    name     VARCHAR(100) NOT NULL COMMENT '货架名称',
    status   TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_area_code (area_id, code),
    KEY idx_area_id (area_id)
) ENGINE=InnoDB COMMENT='货架表';

-- ============================================================
-- 4. 库存相关
-- ============================================================

-- 当前库存表
CREATE TABLE IF NOT EXISTS stock (
    id            BIGINT  PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
    sku_id        BIGINT  NOT NULL COMMENT 'SKU ID',
    warehouse_id  BIGINT  NOT NULL COMMENT '仓库ID',
    quantity      INT     NOT NULL DEFAULT 0 COMMENT '可用数量',
    locked_qty    INT     NOT NULL DEFAULT 0 COMMENT '锁定数量',
    defective_qty INT     NOT NULL DEFAULT 0 COMMENT '次品数量',
    deleted       TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sku_warehouse (sku_id, warehouse_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB COMMENT='当前库存表';

-- 库存流水表
CREATE TABLE IF NOT EXISTS stock_log (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    biz_type       VARCHAR(30)  NOT NULL COMMENT '业务类型：INBOUND/OUTBOUND/RETURN/ADJUST/LOCK/RELEASE',
    biz_no         VARCHAR(50)  NOT NULL COMMENT '业务单号',
    sku_id         BIGINT       NOT NULL COMMENT 'SKU ID',
    warehouse_id   BIGINT       NOT NULL COMMENT '仓库ID',
    quantity_before INT         NOT NULL COMMENT '变动前数量',
    quantity_change INT         NOT NULL COMMENT '变动数量（正数入库，负数出库）',
    quantity_after  INT         NOT NULL COMMENT '变动后数量',
    operator_id    BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    remark         VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_biz (biz_type, biz_no),
    KEY idx_sku_id (sku_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='库存流水表';

-- 库存盘点单表
CREATE TABLE IF NOT EXISTS stock_check (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '盘点单ID',
    check_no      VARCHAR(50)  NOT NULL COMMENT '盘点单号',
    warehouse_id  BIGINT       NOT NULL COMMENT '仓库ID',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待盘点 1-盘点中 2-已完成',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    operator_id   BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_check_no (check_no)
) ENGINE=InnoDB COMMENT='库存盘点单表';

-- 库存盘点明细表
CREATE TABLE IF NOT EXISTS stock_check_item (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    check_id       BIGINT NOT NULL COMMENT '盘点单ID',
    sku_id         BIGINT NOT NULL COMMENT 'SKU ID',
    system_qty     INT    NOT NULL COMMENT '系统库存数量',
    actual_qty     INT    DEFAULT NULL COMMENT '实际盘点数量',
    diff_qty       INT    DEFAULT NULL COMMENT '差异（实际-系统）',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_check_id (check_id)
) ENGINE=InnoDB COMMENT='库存盘点明细表';

-- ============================================================
-- 5. 订单相关
-- ============================================================

-- 订单表
CREATE TABLE IF NOT EXISTS sales_order (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no          VARCHAR(50)   NOT NULL COMMENT '订单号',
    platform_order_no VARCHAR(50)   DEFAULT NULL COMMENT '平台订单号',
    warehouse_id      BIGINT        DEFAULT NULL COMMENT '仓库ID',
    receiver_name     VARCHAR(50)   DEFAULT NULL COMMENT '收件人姓名',
    receiver_phone    VARCHAR(20)   DEFAULT NULL COMMENT '收件人电话',
    receiver_address  VARCHAR(500)  DEFAULT NULL COMMENT '收件人地址',
    order_status      VARCHAR(30)   NOT NULL DEFAULT 'WAIT_PAY' COMMENT '订单状态',
    total_amount      DECIMAL(12,2) DEFAULT 0.00 COMMENT '订单总金额',
    remark            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    paid_at           DATETIME      DEFAULT NULL COMMENT '付款时间',
    shipped_at        DATETIME      DEFAULT NULL COMMENT '发货时间',
    finished_at       DATETIME      DEFAULT NULL COMMENT '完成时间',
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_platform_order_no (platform_order_no),
    KEY idx_order_status (order_status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS sales_order_item (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    order_id      BIGINT        NOT NULL COMMENT '订单ID',
    sku_id        BIGINT        NOT NULL COMMENT 'SKU ID',
    sku_code      VARCHAR(50)   NOT NULL COMMENT 'SKU编码',
    sku_name      VARCHAR(200)  NOT NULL COMMENT 'SKU名称',
    quantity      INT           NOT NULL COMMENT '数量',
    unit_price    DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_price   DECIMAL(12,2) NOT NULL COMMENT '小计',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_id (order_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB COMMENT='订单明细表';

-- ============================================================
-- 6. 出库相关
-- ============================================================

-- 出库单表
CREATE TABLE IF NOT EXISTS outbound_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '出库单ID',
    outbound_no     VARCHAR(50)  NOT NULL COMMENT '出库单号',
    order_id        BIGINT       NOT NULL COMMENT '关联订单ID',
    order_no        VARCHAR(50)  NOT NULL COMMENT '关联订单号',
    warehouse_id    BIGINT       NOT NULL COMMENT '仓库ID',
    status          VARCHAR(30)  NOT NULL DEFAULT 'WAIT_PICKING' COMMENT '状态：WAIT_PICKING/PICKING/SHIPPED',
    picker_id       BIGINT       DEFAULT NULL COMMENT '拣货人ID',
    picker_name     VARCHAR(50)  DEFAULT NULL COMMENT '拣货人姓名',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    shipped_at      DATETIME     DEFAULT NULL COMMENT '发货时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_outbound_no (outbound_no),
    KEY idx_order_id (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='出库单表';

-- 出库明细表
CREATE TABLE IF NOT EXISTS outbound_order_item (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    outbound_id    BIGINT       NOT NULL COMMENT '出库单ID',
    sku_id         BIGINT       NOT NULL COMMENT 'SKU ID',
    sku_code       VARCHAR(50)  NOT NULL COMMENT 'SKU编码',
    sku_name       VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    quantity       INT          NOT NULL COMMENT '应出数量',
    picked_qty     INT          DEFAULT 0 COMMENT '已拣数量',
    shelf_id       BIGINT       DEFAULT NULL COMMENT '拣货货架ID',
    scanned        TINYINT      DEFAULT 0 COMMENT '是否已扫码确认：0-否 1-是',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_outbound_id (outbound_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB COMMENT='出库明细表';

-- ============================================================
-- 7. 退货相关
-- ============================================================

-- 退货单表
CREATE TABLE IF NOT EXISTS return_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '退货单ID',
    return_no       VARCHAR(50)  NOT NULL COMMENT '退货单号',
    order_id        BIGINT       NOT NULL COMMENT '原订单ID',
    order_no        VARCHAR(50)  NOT NULL COMMENT '原订单号',
    warehouse_id    BIGINT       NOT NULL COMMENT '仓库ID',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_CHECK' COMMENT '状态：PENDING_CHECK/SELLABLE/DEFECTIVE/SCRAPPED',
    reason          VARCHAR(500) DEFAULT NULL COMMENT '退货原因',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    operator_id     BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_return_no (return_no),
    KEY idx_order_id (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='退货单表';

-- 退货明细表
CREATE TABLE IF NOT EXISTS return_order_item (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    return_id      BIGINT       NOT NULL COMMENT '退货单ID',
    sku_id         BIGINT       NOT NULL COMMENT 'SKU ID',
    sku_code       VARCHAR(50)  NOT NULL COMMENT 'SKU编码',
    sku_name       VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    quantity       INT          NOT NULL COMMENT '退货数量',
    quality_status VARCHAR(30)  DEFAULT NULL COMMENT '质检状态：SELLABLE/DEFECTIVE/SCRAPPED',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_return_id (return_id),
    KEY idx_sku_id (sku_id)
) ENGINE=InnoDB COMMENT='退货明细表';

-- ============================================================
-- 8. 系统相关
-- ============================================================

-- 文件记录表
CREATE TABLE IF NOT EXISTS file_record (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    file_name     VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_path     VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size     BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
    file_type     VARCHAR(50)  DEFAULT NULL COMMENT '文件类型',
    biz_type      VARCHAR(30)  DEFAULT NULL COMMENT '业务类型：PRODUCT/RETURN/OUTBOUND',
    biz_id        BIGINT       DEFAULT NULL COMMENT '业务ID',
    uploader_id   BIGINT       DEFAULT NULL COMMENT '上传人ID',
    uploader_name VARCHAR(50)  DEFAULT NULL COMMENT '上传人姓名',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB COMMENT='文件记录表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id        BIGINT       DEFAULT NULL COMMENT '操作人ID',
    user_name      VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    module         VARCHAR(50)  NOT NULL COMMENT '模块',
    action         VARCHAR(50)  NOT NULL COMMENT '操作',
    target_type    VARCHAR(50)  DEFAULT NULL COMMENT '操作对象类型',
    target_id      BIGINT       DEFAULT NULL COMMENT '操作对象ID',
    detail         TEXT         DEFAULT NULL COMMENT '操作详情',
    ip             VARCHAR(50)  DEFAULT NULL COMMENT 'IP地址',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ES 同步任务表
CREATE TABLE IF NOT EXISTS es_sync_task (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    biz_type    VARCHAR(30)  NOT NULL COMMENT '业务类型：ORDER',
    biz_id      BIGINT       NOT NULL COMMENT '业务ID',
    operation   VARCHAR(20)  NOT NULL COMMENT '操作：CREATE/UPDATE/DELETE',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SYNCING/SUCCESS/FAILED',
    retry_count INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_msg   TEXT         DEFAULT NULL COMMENT '错误信息',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='ES同步任务表';

-- ============================================================
-- 9. 初始化数据
-- ============================================================

-- 初始化角色
INSERT INTO sys_role (role_code, role_name, description) VALUES
('SUPER_ADMIN', '超级管理员', '全部权限'),
('WAREHOUSE_ADMIN', '仓库管理员', '入库、出库、退货、库存盘点'),
('OPERATOR', '仓库操作员', '扫码入库、扫码出库、库存查询'),
('VIEWER', '查询员', '只读查询');

-- 初始化超级管理员（密码：123456，BCrypt加密）
INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1);

-- 给admin分配超级管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'SUPER_ADMIN';
