package com.yiweilai.wms.outbound.dto;

import lombok.Data;

/**
 * 出库单查询参数
 */
@Data
public class OutboundQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 出库单号 */
    private String outboundNo;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 快递单号 */
    private String trackingNo;

    /** 状态 */
    private String status;

    /** 仓库ID */
    private Long warehouseId;
}
