package com.yiweilai.wms.system.scheduler;

import com.yiweilai.wms.system.entity.BackupConfig;
import com.yiweilai.wms.system.entity.BackupRecord;
import com.yiweilai.wms.system.mapper.BackupConfigMapper;
import com.yiweilai.wms.system.service.BackupService;
import com.yiweilai.wms.system.service.RemoteBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 自动备份调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;
    private final BackupConfigMapper backupConfigMapper;
    private final RemoteBackupService remoteBackupService;

    /**
     * 每天凌晨2点检查是否需要自动备份
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackup() {
        BackupConfig config = backupConfigMapper.getConfig();
        if (config == null || !Boolean.TRUE.equals(config.getAutoBackupEnabled())) {
            return;
        }

        log.info("开始自动备份: type={}", config.getAutoBackupType());

        try {
            BackupRecord record;
            if ("INCREMENTAL".equalsIgnoreCase(config.getAutoBackupType())) {
                record = backupService.incrementalBackup(null, config.getBackupPath());
            } else {
                record = backupService.fullBackup(config.getBackupPath());
            }

            log.info("自动备份完成: file={}", record.getFileName());

            // 如果启用了远程备份，上传到远程服务器
            if (Boolean.TRUE.equals(config.getRemoteBackupEnabled())) {
                File file = new File(record.getFilePath());
                if (file.exists()) {
                    boolean uploaded = remoteBackupService.uploadToRemote(file, config);
                    if (uploaded) {
                        log.info("远程备份上传成功");
                    } else {
                        log.warn("远程备份上传失败");
                    }
                }
            }
        } catch (Exception e) {
            log.error("自动备份失败: {}", e.getMessage(), e);
        }
    }
}
