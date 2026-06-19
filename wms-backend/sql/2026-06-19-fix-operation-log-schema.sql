-- 操作日志表结构迁移
-- 将旧的 operation/method/params/result/status/error_msg 字段
-- 迁移为新的 module/action/target_type/target_id/detail 字段

-- 检查旧列是否存在，如果存在则执行迁移
-- 注意：如果表是新建的（已按 schema.sql 创建），可以跳过此脚本

-- 1. 添加新列
ALTER TABLE operation_log
    ADD COLUMN IF NOT EXISTS module VARCHAR(50) NOT NULL DEFAULT '' COMMENT '模块' AFTER user_name,
    ADD COLUMN IF NOT EXISTS action VARCHAR(50) NOT NULL DEFAULT '' COMMENT '操作' AFTER module,
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型' AFTER action,
    ADD COLUMN IF NOT EXISTS target_id BIGINT DEFAULT NULL COMMENT '操作对象ID' AFTER target_type,
    ADD COLUMN IF NOT EXISTS detail TEXT DEFAULT NULL COMMENT '操作详情' AFTER target_id;

-- 2. 迁移旧数据（如果旧列存在）
-- 将 operation 字段迁移到 module
UPDATE operation_log SET module = 'unknown' WHERE (module IS NULL OR module = '') AND operation IS NOT NULL;

-- 3. 删除旧列（如果存在）
ALTER TABLE operation_log
    DROP COLUMN IF EXISTS operation,
    DROP COLUMN IF EXISTS method,
    DROP COLUMN IF EXISTS params,
    DROP COLUMN IF EXISTS result,
    DROP COLUMN IF EXISTS status,
    DROP COLUMN IF EXISTS error_msg;

-- 4. 添加索引
ALTER TABLE operation_log
    ADD INDEX IF NOT EXISTS idx_module_action (module, action);
