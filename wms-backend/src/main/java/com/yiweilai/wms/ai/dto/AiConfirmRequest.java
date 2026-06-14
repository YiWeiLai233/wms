package com.yiweilai.wms.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiConfirmRequest {

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @NotBlank(message = "操作编码不能为空")
    private String actionCode;

    private Boolean confirmed;
}
