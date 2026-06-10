package com.yiweilai.wms.stock.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存返回对象
 */
@Data
public class StockVO {

    /** 库存ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 可用数量 */
    private Integer quantity;

    /** 锁定数量 */
    private Integer lockedQty;

    /** 次品数量 */
    private Integer defectiveQty;

    /** 总数量（可用+锁定+次品） */
    private Integer totalQuantity;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
