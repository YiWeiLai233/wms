package com.yiweilai.wms.alert.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存预警状态返回对象（低库存/缺货商品列表）
 */
@Data
public class StockAlertStatusVO {

    /** 库存ID */
    private Long stockId;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 当前库存 */
    private Integer quantity;

    /** 锁定库存 */
    private Integer lockedQty;

    /** 低库存阈值 */
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    private Integer outOfStockThreshold;

    /** 预警状态：NORMAL/LOW_STOCK/OUT_OF_STOCK */
    private String alertStatus;

    /** 预警状态名称 */
    private String alertStatusName;

    /** 库存更新时间 */
    private LocalDateTime updatedAt;
}
