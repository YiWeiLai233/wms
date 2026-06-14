package com.yiweilai.wms.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiKnowledgeDocument {

    private Long id;

    private String title;

    private Long fileId;

    private String fileName;

    private String filePath;

    private String sourceType;

    private String status;

    private Integer chunkCount;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
