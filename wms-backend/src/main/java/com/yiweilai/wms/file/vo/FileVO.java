package com.yiweilai.wms.file.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件返回对象
 */
@Data
public class FileVO {

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

    /** 访问URL */
    private String url;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
