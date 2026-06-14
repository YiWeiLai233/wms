package com.yiweilai.wms.ai.client;

import com.yiweilai.wms.ai.dto.AiChatResponse;
import com.yiweilai.wms.ai.dto.AiKnowledgeIngestRequest;
import com.yiweilai.wms.ai.dto.AiServiceChatRequest;

public interface AiServiceClient {

    AiChatResponse chat(AiServiceChatRequest request);

    void ingestKnowledge(AiKnowledgeIngestRequest request);

    void deleteKnowledge(Long documentId);
}
