package com.yiweilai.wms.stock.dto;

import lombok.Data;

/**
 * 库存流水查询参数
 */
@Data
public class StockLogQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 业务类型 */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** 仓库ID */
    private Long warehouseId;

    /** 开始时间，格式 yyyy-MM-dd HH:mm:ss */
    private String startTime;

    /** 结束时间，格式 yyyy-MM-dd HH:mm:ss */
    private String endTime;
}
