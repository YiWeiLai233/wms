package com.yiweilai.wms.ai.controller;

import com.yiweilai.wms.ai.dto.AiChatRequest;
import com.yiweilai.wms.ai.dto.AiChatResponse;
import com.yiweilai.wms.ai.entity.AiConversation;
import com.yiweilai.wms.ai.entity.AiMessage;
import com.yiweilai.wms.ai.service.AiChatService;
import com.yiweilai.wms.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.yiweilai.wms.security.RequirePermission;

@RequirePermission("ai.assistant")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(userId, request));
    }

    @GetMapping("/conversations")
    public Result<List<AiConversation>> listConversations(@RequestAttribute("userId") Long userId) {
        return Result.success(aiChatService.listConversations(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<AiMessage>> listMessages(@RequestAttribute("userId") Long userId,
                                                @PathVariable Long id) {
        return Result.success(aiChatService.listMessages(userId, id));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@RequestAttribute("userId") Long userId,
                                           @PathVariable Long id) {
        aiChatService.deleteConversation(userId, id);
        return Result.success();
    }
}
