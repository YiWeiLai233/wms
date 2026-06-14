package com.yiweilai.wms.ai.dto.tool;

import lombok.Data;

@Data
public class AiOrderSearchRequest {

    private String keyword;

    private String orderNo;

    private String platformOrderNo;

    private String receiverName;

    private String receiverPhone;

    private String orderStatus;

    private Long warehouseId;

    private Integer page = 1;

    private Integer size = 10;
}
