package com.yiweilai.wms.ai.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolContext {

    private Long userId;

    private Long conversationId;

    private Long messageId;

    public static AiToolContext of(Long userId, Long conversationId, Long messageId) {
        return new AiToolContext(userId, conversationId, messageId);
    }
}
