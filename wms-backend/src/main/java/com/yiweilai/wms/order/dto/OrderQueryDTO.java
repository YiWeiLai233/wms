package com.yiweilai.wms.order.dto;

import lombok.Data;

/**
 * 订单查询参数
 */
@Data
public class OrderQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 订单状态 */
    private String orderStatus;

    /** 仓库ID */
    private Long warehouseId;
}
