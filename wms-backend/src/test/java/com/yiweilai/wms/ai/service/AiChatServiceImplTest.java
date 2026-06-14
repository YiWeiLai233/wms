package com.yiweilai.wms.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.client.AiServiceClient;
import com.yiweilai.wms.ai.dto.AiChatRequest;
import com.yiweilai.wms.ai.dto.AiChatResponse;
import com.yiweilai.wms.ai.entity.AiConversation;
import com.yiweilai.wms.ai.entity.AiMessage;
import com.yiweilai.wms.ai.mapper.AiConversationMapper;
import com.yiweilai.wms.ai.mapper.AiMessageMapper;
import com.yiweilai.wms.ai.service.impl.AiChatServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceImplTest {

    @Test
    void chatCreatesConversationAndPersistsUserAndAssistantMessages() {
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiMessageMapper messageMapper = mock(AiMessageMapper.class);
        AiServiceClient aiServiceClient = mock(AiServiceClient.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);

        when(conversationMapper.insert(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            conversation.setId(99L);
            return 1;
        });
        when(aiServiceClient.chat(any())).thenReturn(AiChatResponse.builder()
                .answer("根据知识库，收货异常需要先拍照留证。")
                .needConfirm(false)
                .build());

        AiChatServiceImpl service = new AiChatServiceImpl(
                conversationMapper,
                messageMapper,
                aiServiceClient,
                toolLogService,
                new ObjectMapper());

        AiChatRequest request = new AiChatRequest();
        request.setMessage("收货异常怎么处理？");
        request.setMode("knowledge");

        AiChatResponse response = service.chat(7L, request);

        assertThat(response.getConversationId()).isEqualTo(99L);
        assertThat(response.getAnswer()).contains("收货异常");
        verify(conversationMapper).insert(any(AiConversation.class));
        verify(messageMapper).insert(org.mockito.ArgumentMatchers.argThat(message ->
                Long.valueOf(99L).equals(message.getConversationId())
                        && Long.valueOf(7L).equals(message.getUserId())
                        && "user".equals(message.getRole())
                        && "收货异常怎么处理？".equals(message.getContent())));
        verify(messageMapper).insert(org.mockito.ArgumentMatchers.argThat(message ->
                Long.valueOf(99L).equals(message.getConversationId())
                        && "assistant".equals(message.getRole())
                        && message.getContent().contains("收货异常")));
    }
}
