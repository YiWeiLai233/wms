package com.yiweilai.wms.stock.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 当前库存实体
 */
@Data
public class Stock {

    /** 库存ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    private String skuCode;

    private String skuName;

    /** 商品ID */
    private Long productId;

    private String productName;

    /** 仓库ID */
    private Long warehouseId;

    private String warehouseName;

    /** 可用数量 */
    private Integer quantity;

    /** 锁定数量 */
    private Integer lockedQty;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
