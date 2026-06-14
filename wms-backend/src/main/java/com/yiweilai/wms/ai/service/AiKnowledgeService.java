package com.yiweilai.wms.ai.service;

import com.yiweilai.wms.ai.dto.AiKnowledgeIngestResult;
import com.yiweilai.wms.ai.entity.AiKnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiKnowledgeService {

    AiKnowledgeDocument upload(Long userId, MultipartFile file);

    List<AiKnowledgeDocument> list(Long userId);

    AiKnowledgeDocument getById(Long userId, Long id);

    void delete(Long userId, Long id);

    AiKnowledgeDocument rebuild(Long userId, Long id);

    void completeIngestion(Long documentId, AiKnowledgeIngestResult result);
}
