package com.yiweilai.wms.system.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库备份记录
 */
@Data
public class BackupRecord {

    /** 备份ID */
    private Long id;

    /** 备份类型：FULL-全量 INCREMENTAL-增量 */
    private String backupType;

    /** 文件名 */
    private String fileName;

    /** 文件路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 状态：SUCCESS/FAILED */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
