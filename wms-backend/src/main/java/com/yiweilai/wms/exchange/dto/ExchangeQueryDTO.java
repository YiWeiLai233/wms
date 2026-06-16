package com.yiweilai.wms.exchange.dto;

import lombok.Data;

/**
 * 换货单查询参数
 */
@Data
public class ExchangeQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 换货单号 */
    private String exchangeNo;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 状态 */
    private String status;

    /** 仓库ID */
    private Long warehouseId;
}
