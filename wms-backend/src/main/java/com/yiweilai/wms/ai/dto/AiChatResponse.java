package com.yiweilai.wms.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String answer;

    private Long conversationId;

    @Builder.Default
    private Boolean needConfirm = false;

    @Builder.Default
    private List<AiSourceDTO> sources = new ArrayList<>();

    @Builder.Default
    private List<AiToolCallDTO> toolCalls = new ArrayList<>();
}
