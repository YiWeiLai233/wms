package com.yiweilai.wms.ai.dto.tool;

import lombok.Data;

@Data
public class AiReturnQueryRequest {

    private Long id;

    private String returnNo;

    private String orderNo;

    private String platformOrderNo;

    private String status;

    private Long warehouseId;

    private Integer page = 1;

    private Integer size = 10;
}
