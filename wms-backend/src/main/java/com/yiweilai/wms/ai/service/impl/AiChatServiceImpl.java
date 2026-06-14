package com.yiweilai.wms.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.client.AiServiceClient;
import com.yiweilai.wms.ai.dto.AiChatRequest;
import com.yiweilai.wms.ai.dto.AiChatResponse;
import com.yiweilai.wms.ai.dto.AiMessageDTO;
import com.yiweilai.wms.ai.dto.AiServiceChatRequest;
import com.yiweilai.wms.ai.entity.AiConversation;
import com.yiweilai.wms.ai.entity.AiMessage;
import com.yiweilai.wms.ai.mapper.AiConversationMapper;
import com.yiweilai.wms.ai.mapper.AiMessageMapper;
import com.yiweilai.wms.ai.service.AiChatService;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiServiceClient aiServiceClient;
    private final AiToolLogService toolLogService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        AiConversation conversation = resolveConversation(userId, request);

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversation.getId());
        userMessage.setUserId(userId);
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        messageMapper.insert(userMessage);

        List<AiMessageDTO> history = messageMapper.findByConversationId(conversation.getId()).stream()
                .map(message -> new AiMessageDTO(message.getRole(), message.getContent()))
                .toList();

        AiChatResponse response;
        try {
            response = aiServiceClient.chat(AiServiceChatRequest.builder()
                    .conversationId(conversation.getId())
                    .userId(userId)
                    .message(request.getMessage())
                    .mode(request.getMode())
                    .history(history)
                    .build());
        } catch (RuntimeException e) {
            log.warn("AI服务调用失败: {}", e.getMessage());
            response = AiChatResponse.builder()
                    .answer("AI服务暂不可用，请稍后重试。")
                    .needConfirm(false)
                    .build();
        }

        response.setConversationId(conversation.getId());
        if (response.getNeedConfirm() == null) {
            response.setNeedConfirm(false);
        }

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setUserId(null);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(response.getAnswer());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("needConfirm", response.getNeedConfirm());
        metadata.put("sources", response.getSources() == null ? List.of() : response.getSources());
        metadata.put("toolCalls", response.getToolCalls() == null ? List.of() : response.getToolCalls());
        metadata.put("pendingAction", response.getPendingAction());
        assistantMessage.setMetadata(toJson(metadata));
        messageMapper.insert(assistantMessage);

        toolLogService.recordToolCalls(userId, conversation.getId(), assistantMessage.getId(), response.getToolCalls());
        conversationMapper.touch(conversation.getId());
        return response;
    }

    @Override
    public List<AiConversation> listConversations(Long userId) {
        return conversationMapper.findByUserId(userId);
    }

    @Override
    public List<AiMessage> listMessages(Long userId, Long conversationId) {
        ensureConversation(userId, conversationId);
        return messageMapper.findByConversationId(conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long userId, Long conversationId) {
        ensureConversation(userId, conversationId);
        messageMapper.deleteByConversationId(conversationId);
        conversationMapper.deleteByIdAndUserId(conversationId, userId);
    }

    private AiConversation resolveConversation(Long userId, AiChatRequest request) {
        if (request.getConversationId() != null) {
            return ensureConversation(userId, request.getConversationId());
        }

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(buildTitle(request.getMessage()));
        conversationMapper.insert(conversation);
        return conversation;
    }

    private AiConversation ensureConversation(Long userId, Long conversationId) {
        AiConversation conversation = conversationMapper.findByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI会话不存在");
        }
        return conversation;
    }

    private String buildTitle(String message) {
        String text = message == null ? "新会话" : message.trim();
        if (text.isEmpty()) {
            return "新会话";
        }
        return text.length() > 30 ? text.substring(0, 30) + "..." : text;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
