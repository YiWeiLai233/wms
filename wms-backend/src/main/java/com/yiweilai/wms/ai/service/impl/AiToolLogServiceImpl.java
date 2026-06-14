package com.yiweilai.wms.ai.service.impl;

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
}
