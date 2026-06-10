package com.yiweilai.wms.file.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件记录实体
 */
@Data
public class FileRecord {

    /** 文件ID */
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型 */
    private String fileType;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 上传人ID */
    private Long uploaderId;

    /** 上传人姓名 */
    private String uploaderName;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
