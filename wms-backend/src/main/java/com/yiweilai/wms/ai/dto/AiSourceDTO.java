package com.yiweilai.wms.ai.dto;

import lombok.Data;

@Data
public class AiSourceDTO {

    private Long documentId;

    private String title;

    private String content;

    private Double score;
}
