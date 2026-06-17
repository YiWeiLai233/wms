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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 每分钟检查一次，匹配到配置时间时执行备份
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndBackup() {
        BackupConfig config = backupConfigMapper.getConfig();
        if (config == null || !Boolean.TRUE.equals(config.getAutoBackupEnabled())) {
            return;
        }

        // 检查当前时间是否匹配配置的备份时间
        String backupTime = config.getAutoBackupTime();
        if (backupTime == null || backupTime.isBlank()) {
            return;
        }

        String now = LocalTime.now().format(TIME_FMT);
        if (!now.equals(backupTime)) {
            return;
        }

        doBackup(config);
    }

    private void doBackup(BackupConfig config) {
        log.info("开始自动备份: type={}, time={}", config.getAutoBackupType(), config.getAutoBackupTime());

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
