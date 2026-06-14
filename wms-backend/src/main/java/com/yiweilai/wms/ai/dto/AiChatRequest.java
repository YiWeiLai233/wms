package com.yiweilai.wms.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {

    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String mode = "knowledge";
}
