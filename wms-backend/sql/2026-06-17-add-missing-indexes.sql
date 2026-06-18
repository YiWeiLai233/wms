-- 添加缺失的数据库索引
-- 执行前请先备份数据库

-- 1. product_category 表添加 parent_id 索引（用于树形查询）
ALTER TABLE product_category ADD INDEX idx_parent_id (parent_id);

-- 2. stock_log 表添加 warehouse_id 索引（用于按仓库查询流水）
ALTER TABLE stock_log ADD INDEX idx_warehouse_id (warehouse_id);

-- 3. operation_log 表添加 module+action 组合索引（用于按模块和操作查询）
-- 注意：需要先确认 operation_log 表是否存在
-- ALTER TABLE operation_log ADD INDEX idx_module_action (module, action);

-- 4. sales_order_item 添加 order_id 索引（如果不存在）
-- 注意：外键约束会自动创建索引，但如果没有外键，需要手动添加
-- ALTER TABLE sales_order_item ADD INDEX idx_order_id (order_id);

-- 5. outbound_order 添加 order_id 索引（如果不存在）
-- ALTER TABLE outbound_order ADD INDEX idx_order_id (order_id);

-- 6. return_order 添加 order_id 索引（如果不存在）
-- ALTER TABLE return_order ADD INDEX idx_order_id (order_id);
