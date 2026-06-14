package com.yiweilai.wms.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.dto.AiToolCallDTO;
import com.yiweilai.wms.ai.entity.AiToolCallLog;
import com.yiweilai.wms.ai.mapper.AiToolCallLogMapper;
import com.yiweilai.wms.ai.service.AiToolLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiToolLogServiceImpl implements AiToolLogService {

    private final AiToolCallLogMapper toolCallLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void recordToolCalls(Long userId, Long conversationId, Long messageId, List<AiToolCallDTO> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        for (AiToolCallDTO toolCall : toolCalls) {
            AiToolCallLog log = new AiToolCallLog();
            log.setUserId(userId);
            log.setConversationId(conversationId);
            log.setMessageId(messageId);
            log.setToolName(toolCall.getToolName());
            log.setRequestParams(toolCall.getRequestParams());
            log.setResponseData(toolCall.getResponseData());
            log.setStatus(toolCall.getStatus() == null ? "SUCCESS" : toolCall.getStatus());
            log.setErrorMessage(toolCall.getErrorMessage());
            toolCallLogMapper.insert(log);
        }
    }

    @Override
    public void recordToolCall(Long userId,
                               Long conversationId,
                               Long messageId,
                               String toolName,
                               Object request,
                               Object response,
                               String status,
                               String errorMessage) {
        AiToolCallDTO toolCall = new AiToolCallDTO();
        toolCall.setToolName(toolName);
        toolCall.setRequestParams(toJson(request));
        toolCall.setResponseData(toJson(response));
        toolCall.setStatus(status);
        toolCall.setErrorMessage(errorMessage);
        recordToolCalls(userId, conversationId, messageId, List.of(toolCall));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
