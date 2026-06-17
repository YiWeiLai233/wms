-- 权限数据初始化
-- 一级菜单权限
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

-- 获取一级菜单ID
SET @ai_id = (SELECT id FROM sys_permission WHERE permission_code = 'ai' AND deleted = 0 LIMIT 1);
SET @warehouse_id = (SELECT id FROM sys_permission WHERE permission_code = 'warehouse' AND deleted = 0 LIMIT 1);
SET @product_id = (SELECT id FROM sys_permission WHERE permission_code = 'product' AND deleted = 0 LIMIT 1);
SET @stock_id = (SELECT id FROM sys_permission WHERE permission_code = 'stock' AND deleted = 0 LIMIT 1);
SET @order_id = (SELECT id FROM sys_permission WHERE permission_code = 'order' AND deleted = 0 LIMIT 1);
SET @express_id = (SELECT id FROM sys_permission WHERE permission_code = 'express' AND deleted = 0 LIMIT 1);
SET @system_id = (SELECT id FROM sys_permission WHERE permission_code = 'system' AND deleted = 0 LIMIT 1);

-- 二级菜单权限
INSERT INTO sys_permission (permission_code, permission_name, parent_id, type, sort_order) VALUES
-- 智能助手
('ai.assistant', 'AI 助手', @ai_id, 1, 1),
('ai.knowledge', '知识库管理', @ai_id, 1, 2),
-- 仓库管理
('warehouse.list', '仓库管理', @warehouse_id, 1, 1),
('warehouse.shelf', '货架管理', @warehouse_id, 1, 2),
('warehouse.special', '特殊仓库管理', @warehouse_id, 1, 3),
-- 商品管理
('product.list', '商品列表', @product_id, 1, 1),
('product.sku', 'SKU管理', @product_id, 1, 2),
-- 库存管理
('stock.query', '库存查询', @stock_id, 1, 1),
('stock.log', '库存流水', @stock_id, 1, 2),
('stock.check', '盘点管理', @stock_id, 1, 3),
-- 订单中心
('order.list', '订单管理', @order_id, 1, 1),
('outbound.list', '发货管理', @order_id, 1, 2),
('returns.list', '退货管理', @order_id, 1, 3),
('exchange.list', '换货管理', @order_id, 1, 4),
-- 快递管理
('express.query', '快递查询', @express_id, 1, 1),
('express.company', '快递公司管理', @express_id, 1, 2),
('express.template', '费用模板管理', @express_id, 1, 3),
('express.report', '快递费用统计', @express_id, 1, 4),
-- 系统管理
('system.users', '用户管理', @system_id, 1, 1),
('system.roles', '角色管理', @system_id, 1, 2),
('system.files', '文件管理', @system_id, 1, 3),
('system.logs', '操作日志', @system_id, 1, 4),
('system.stock-alert', '库存预警设置', @system_id, 1, 5),
('system.stock-alert-template', '预警模板管理', @system_id, 1, 6),
('system.platforms', '平台管理', @system_id, 1, 7),
('system.backup', '数据库备份', @system_id, 1, 8)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 为超级管理员分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND r.deleted = 0 AND p.deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
