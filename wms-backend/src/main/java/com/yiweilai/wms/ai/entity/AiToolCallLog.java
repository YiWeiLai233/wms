package com.yiweilai.wms.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiToolCallLog {

    private Long id;

    private Long userId;

    private Long conversationId;

    private Long messageId;

    private String toolName;

    private String requestParams;

    private String responseData;

    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;
}
