package com.yiweilai.wms.returns.dto;

import lombok.Data;

/**
 * 退货单查询参数
 */
@Data
public class ReturnQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 退货单号 */
    private String returnNo;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 状态 */
    private String status;

    /** 仓库ID */
    private Long warehouseId;
}
