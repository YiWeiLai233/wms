package com.yiweilai.wms.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversation {

    private Long id;

    private Long userId;

    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
