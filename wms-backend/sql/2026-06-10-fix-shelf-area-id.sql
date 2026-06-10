-- 修改 warehouse_shelf 表的 area_id 字段，允许为空或设置默认值

-- 方法1：将 area_id 字段改为允许为空
ALTER TABLE warehouse_shelf
    MODIFY COLUMN area_id BIGINT DEFAULT NULL COMMENT '库区ID（已废弃）';

-- 方法2：如果不需要 area_id 字段，可以直接删除
-- ALTER TABLE warehouse_shelf DROP COLUMN area_id;
