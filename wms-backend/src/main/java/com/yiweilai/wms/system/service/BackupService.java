package com.yiweilai.wms.system.service;

import com.yiweilai.wms.system.entity.BackupRecord;

import java.io.File;
import java.util.List;

/**
 * 数据库备份 Service
 */
public interface BackupService {

    /**
     * 全量备份
     * @param backupPath 服务器保存路径（可选，为空则使用默认路径）
     * @return 备份记录
     */
    BackupRecord fullBackup(String backupPath);

    /**
     * 增量备份（基于指定全量备份以来的变更）
     * @param baseBackupId 基准全量备份ID，null则自动取最近一次备份时间
     * @param backupPath 服务器保存路径（可选）
     * @return 备份记录
     */
    BackupRecord incrementalBackup(Long baseBackupId, String backupPath);

    /**
     * 查询备份记录列表
     */
    List<BackupRecord> getBackupList();

    /**
     * 获取备份文件
     * @param fileName 文件名
     * @return 文件对象
     */
    File getBackupFile(String fileName);

    /**
     * 删除备份记录及文件
     */
    void deleteBackup(Long id);
}
