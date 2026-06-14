package com.yiweilai.wms.ai.dto;

import lombok.Data;

@Data
public class AiKnowledgeChunkDTO {

    private Integer chunkIndex;

    private String content;

    private String vectorId;

    private String metadata;
}
