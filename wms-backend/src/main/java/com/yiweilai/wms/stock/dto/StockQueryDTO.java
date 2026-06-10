package com.yiweilai.wms.stock.dto;

import lombok.Data;

/**
 * 库存查询参数
 */
@Data
public class StockQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称（模糊查询） */
    private String skuName;

    /** 商品名称（模糊查询） */
    private String productName;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 库位编码（模糊查询） */
    private String locationCode;

    /** 库存类型：all-全部, normal-正常, low-低库存, out-缺货 */
    private String stockType;
}
