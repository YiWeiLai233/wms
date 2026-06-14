package com.yiweilai.wms.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiServiceChatRequest {

    private Long conversationId;

    private Long userId;

    private String message;

    private String mode;

    private List<AiMessageDTO> history;
}
