package com.yiweilai.wms.stock.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存盘点明细实体
 */
@Data
public class StockCheckItem {

    /** 明细ID */
    private Long id;

    /** 盘点单ID */
    private Long checkId;

    /** SKU ID */
    private Long skuId;

    /** 系统库存数量 */
    private Integer systemQty;

    /** 实际盘点数量 */
    private Integer actualQty;

    /** 差异（实际-系统） */
    private Integer diffQty;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
