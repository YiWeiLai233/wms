package com.yiweilai.wms.alert.dto;

import lombok.Data;

/**
 * 查询库存预警配置参数
 */
@Data
public class StockAlertConfigQueryDTO {

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 仓库ID */
    private Long warehouseId;

    /** 是否启用 */
    private Integer enabled;

    /** 页码 */
    private Integer page = 1;

    /** 每页大小 */
    private Integer size = 10;
}
