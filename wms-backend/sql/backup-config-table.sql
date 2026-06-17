-- 备份配置表
CREATE TABLE IF NOT EXISTS backup_config (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    auto_backup_enabled  TINYINT DEFAULT 0 COMMENT '是否启用自动备份',
    auto_backup_type     VARCHAR(20) DEFAULT 'FULL' COMMENT '备份类型: FULL/INCREMENTAL',
    auto_backup_cron     VARCHAR(50) DEFAULT '0 0 2 * * ?' COMMENT 'cron表达式',
    backup_path          VARCHAR(500) COMMENT '备份路径',
    remote_backup_enabled TINYINT DEFAULT 0 COMMENT '是否启用远程备份',
    remote_host          VARCHAR(100) COMMENT '远程主机',
    remote_port          INT DEFAULT 22 COMMENT 'SSH端口',
    remote_username      VARCHAR(50) COMMENT '远程用户名',
    remote_password      VARCHAR(100) COMMENT '远程密码',
    remote_path          VARCHAR(500) COMMENT '远程路径',
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备份配置表';

-- 插入默认配置
INSERT INTO backup_config (auto_backup_enabled, auto_backup_type, auto_backup_cron, remote_backup_enabled, remote_port)
VALUES (0, 'FULL', '0 0 2 * * ?', 0, 22);
