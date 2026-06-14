package com.yiweilai.wms.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiKnowledgeChunk {

    private Long id;

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private String vectorId;

    private String metadata;

    private LocalDateTime createdAt;
}
