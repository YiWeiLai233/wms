package com.yiweilai.wms.ai.service;

import com.yiweilai.wms.ai.dto.AiToolCallDTO;

import java.util.List;

public interface AiToolLogService {

    void recordToolCalls(Long userId, Long conversationId, Long messageId, List<AiToolCallDTO> toolCalls);

    void recordToolCall(Long userId,
                        Long conversationId,
                        Long messageId,
                        String toolName,
                        Object request,
                        Object response,
                        String status,
                        String errorMessage);
}
