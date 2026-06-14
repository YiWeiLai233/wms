package com.yiweilai.wms.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiMessage {

    private Long id;

    private Long conversationId;

    private Long userId;

    private String role;

    private String content;

    private String metadata;

    private LocalDateTime createdAt;
}
