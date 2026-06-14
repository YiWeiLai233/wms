package com.yiweilai.wms.ai.dto;

import lombok.Data;

@Data
public class AiToolCallDTO {

    private String toolName;

    private String status;

    private String requestParams;

    private String responseData;

    private String errorMessage;
}
