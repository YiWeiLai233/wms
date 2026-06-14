package com.yiweilai.wms.ai.dto.action;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiActionExecuteResult {

    private Long actionId;

    private String actionType;

    private String status;

    private Object resultData;

    private String errorMessage;

    public static AiActionExecuteResult success(Long actionId, String actionType, Object resultData) {
        return AiActionExecuteResult.builder()
                .actionId(actionId)
                .actionType(actionType)
                .status("EXECUTED")
                .resultData(resultData)
                .build();
    }

    public static AiActionExecuteResult failed(Long actionId, String actionType, String errorMessage) {
        return AiActionExecuteResult.builder()
                .actionId(actionId)
                .actionType(actionType)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
