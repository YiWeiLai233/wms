package com.yiweilai.wms.stock.dto;

import lombok.Data;

/**
 * 盘点单查询参数
 */
@Data
public class StockCheckQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 仓库ID */
    private Long warehouseId;

    /** 状态：0-待盘点 1-盘点中 2-已完成 */
    private Integer status;
}
