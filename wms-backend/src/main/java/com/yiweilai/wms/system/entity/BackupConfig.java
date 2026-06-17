package com.yiweilai.wms.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 备份配置实体
 */
@Data
public class BackupConfig {
    private Long id;
    private Boolean autoBackupEnabled;    // 是否启用自动备份
    private String autoBackupType;        // FULL / INCREMENTAL
    private String autoBackupCron;        // cron 表达式
    private String backupPath;            // 备份路径
    private Boolean remoteBackupEnabled;  // 是否启用远程备份
    private String remoteHost;            // 远程主机
    private Integer remotePort;           // 远程端口
    private String remoteUsername;         // 远程用户名
    private String remotePassword;         // 远程密码
    private String remotePath;            // 远程路径
    private LocalDateTime updatedAt;
}
