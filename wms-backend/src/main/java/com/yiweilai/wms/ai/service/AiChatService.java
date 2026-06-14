package com.yiweilai.wms.ai.service;

import com.yiweilai.wms.ai.dto.AiChatRequest;
import com.yiweilai.wms.ai.dto.AiChatResponse;
import com.yiweilai.wms.ai.entity.AiConversation;
import com.yiweilai.wms.ai.entity.AiMessage;

import java.util.List;

public interface AiChatService {

    AiChatResponse chat(Long userId, AiChatRequest request);

    List<AiConversation> listConversations(Long userId);

    List<AiMessage> listMessages(Long userId, Long conversationId);

    void deleteConversation(Long userId, Long conversationId);
}
