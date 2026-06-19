package com.yiweilai.wms.system.schedule;

import com.yiweilai.wms.system.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据库自动备份定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduleTask {

    private final BackupService backupService;

    /**
     * 每天凌晨 3 点自动执行增量备份
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoIncrementalBackup() {
        log.info("开始执行自动增量备份...");
        try {
            backupService.incrementalBackup(null);
            log.info("自动增量备份完成");
        } catch (Exception e) {
            log.error("自动增量备份失败", e);
        }
    }

    /**
     * 每周日凌晨 4 点自动执行全量备份
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void autoFullBackup() {
        log.info("开始执行自动全量备份...");
        try {
            backupService.fullBackup(null);
            log.info("自动全量备份完成");
        } catch (Exception e) {
            log.error("自动全量备份失败", e);
        }
    }
}
