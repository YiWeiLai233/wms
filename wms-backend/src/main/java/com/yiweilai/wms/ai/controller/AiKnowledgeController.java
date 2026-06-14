package com.yiweilai.wms.ai.controller;

import com.yiweilai.wms.ai.entity.AiKnowledgeDocument;
import com.yiweilai.wms.ai.service.AiKnowledgeService;
import com.yiweilai.wms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final AiKnowledgeService knowledgeService;

    @PostMapping("/upload")
    public Result<AiKnowledgeDocument> upload(@RequestAttribute("userId") Long userId,
                                              @RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeService.upload(userId, file));
    }

    @GetMapping("/list")
    public Result<List<AiKnowledgeDocument>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(knowledgeService.list(userId));
    }

    @GetMapping("/{id}")
    public Result<AiKnowledgeDocument> getById(@RequestAttribute("userId") Long userId,
                                               @PathVariable Long id) {
        return Result.success(knowledgeService.getById(userId, id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        knowledgeService.delete(userId, id);
        return Result.success();
    }

    @PostMapping("/{id}/rebuild")
    public Result<AiKnowledgeDocument> rebuild(@RequestAttribute("userId") Long userId,
                                               @PathVariable Long id) {
        return Result.success(knowledgeService.rebuild(userId, id));
    }
}
