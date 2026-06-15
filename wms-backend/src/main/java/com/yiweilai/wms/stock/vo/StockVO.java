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

    /** 可用数量 */
    private Integer quantity;

    /** 锁定数量 */
    private Integer lockedQty;

    /** 总数量（可用+锁定） */
    private Integer totalQuantity;

    /** 库存预警状态：NORMAL/LOW_STOCK/OUT_OF_STOCK */
    private String stockAlertStatus;

    /** 库存预警状态名称 */
    private String stockAlertStatusName;

    /** 低库存阈值 */
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    private Integer outOfStockThreshold;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
