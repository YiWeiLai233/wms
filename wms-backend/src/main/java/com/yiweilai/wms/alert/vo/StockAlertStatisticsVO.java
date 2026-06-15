package com.yiweilai.wms.alert.vo;

import lombok.Data;

/**
 * 库存预警统计返回对象
 */
@Data
public class StockAlertStatisticsVO {

    /** 库存正常SKU数量 */
    private Long normalCount;

    /** 低库存SKU数量 */
    private Long lowStockCount;

    /** 缺货SKU数量 */
    private Long outOfStockCount;
}
