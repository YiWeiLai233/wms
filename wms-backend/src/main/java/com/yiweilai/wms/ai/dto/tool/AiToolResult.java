package com.yiweilai.wms.ai.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiToolResult<T> {

    private String toolName;

    private String status;

    private T data;

    private String summary;

    private String errorMessage;

    public static <T> AiToolResult<T> success(String toolName, T data, String summary) {
        return AiToolResult.<T>builder()
                .toolName(toolName)
                .status("SUCCESS")
                .data(data)
                .summary(summary)
                .build();
    }

    public static <T> AiToolResult<T> failed(String toolName, String errorMessage) {
        return AiToolResult.<T>builder()
                .toolName(toolName)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
