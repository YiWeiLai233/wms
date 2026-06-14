package com.yiweilai.wms.ai.dto.action;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiPendingActionVO {

    private Long actionId;

    private Long userId;

    private Long conversationId;

    private String actionType;

    private String actionName;

    private Object requestParams;

    private String summary;

    private String riskLevel;

    private String status;

    private Boolean needConfirm;

    private LocalDateTime expireAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime executedAt;

    private Object resultData;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
