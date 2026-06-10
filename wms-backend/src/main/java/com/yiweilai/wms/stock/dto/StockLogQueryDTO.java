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

    /** SKU ID */
    private Long skuId;

    /** 仓库ID */
    private Long warehouseId;
}
