package com.yiweilai.wms.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiKnowledgeIngestResult {

    private String status = "SUCCESS";

    private String errorMessage;

    private List<AiKnowledgeChunkDTO> chunks = new ArrayList<>();
}
