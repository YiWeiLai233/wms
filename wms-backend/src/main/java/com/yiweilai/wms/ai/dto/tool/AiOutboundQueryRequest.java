package com.yiweilai.wms.ai.dto.tool;

import lombok.Data;

@Data
public class AiOutboundQueryRequest {

    private Long id;

    private String outboundNo;

    private String orderNo;

    private String platformOrderNo;

    private String trackingNo;

    private String status;

    private Long warehouseId;

    private Integer page = 1;

    private Integer size = 10;
}
