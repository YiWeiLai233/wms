package com.yiweilai.wms.ai.dto.tool;

import lombok.Data;

@Data
public class AiStockQueryRequest {

    private Long skuId;

    private String skuCode;

    private String skuName;

    private String productName;

    private Long warehouseId;

    private String stockType;

    private Integer page = 1;

    private Integer size = 10;
}
