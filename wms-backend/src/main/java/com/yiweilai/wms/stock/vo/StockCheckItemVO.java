package com.yiweilai.wms.stock.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点明细返回对象
 */
@Data
public class StockCheckItemVO {

    /** 明细ID */
    private Long id;

    /** 盘点单ID */
    private Long checkId;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 系统库存数量 */
    private Integer systemQty;

    /** 实际盘点数量 */
    private Integer actualQty;

    /** 差异 */
    private Integer diffQty;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
