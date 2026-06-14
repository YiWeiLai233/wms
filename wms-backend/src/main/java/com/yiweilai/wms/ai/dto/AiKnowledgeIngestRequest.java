package com.yiweilai.wms.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeIngestRequest {

    private Long documentId;

    private String title;

    private String fileName;

    private String filePath;
}
