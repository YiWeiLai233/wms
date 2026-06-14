package com.yiweilai.wms.ai.dto.action;

import lombok.Data;

@Data
public class AiCreateActionRequest {

    private Long userId;

    private Long conversationId;

    private String actionType;

    private String actionName;

    private Object requestParams;

    private String summary;

    private String riskLevel;
}
