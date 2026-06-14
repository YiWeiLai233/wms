package com.yiweilai.wms.ai.controller;

import com.yiweilai.wms.ai.dto.AiKnowledgeIngestResult;
import com.yiweilai.wms.ai.service.AiKnowledgeService;
import com.yiweilai.wms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/internal")
@RequiredArgsConstructor
public class AiInternalController {

    private final AiKnowledgeService knowledgeService;

    @PostMapping("/knowledge/{documentId}/chunks")
    public Result<Void> completeKnowledgeIngestion(@PathVariable Long documentId,
                                                   @RequestBody AiKnowledgeIngestResult result) {
        knowledgeService.completeIngestion(documentId, result);
        return Result.success();
    }
}
