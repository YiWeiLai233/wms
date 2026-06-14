package com.yiweilai.wms.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiPendingAction {

    private Long id;

    private Long userId;

    private Long conversationId;

    private String actionType;

    private String actionName;

    private String requestParams;

    private String summary;

    private String riskLevel;

    private String status;

    private LocalDateTime expireAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime executedAt;

    private String resultData;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
