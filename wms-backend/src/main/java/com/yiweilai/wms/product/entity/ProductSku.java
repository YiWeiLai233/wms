package com.yiweilai.wms.product.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU实体
 */
@Data
public class ProductSku {

    /** SKU ID */
    private Long id;

    /** 商品ID（SPU） */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String name;

    /** 数量 */
    private Integer quantity;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 重量（kg） */
    private BigDecimal weight;

    /** 体积（m³） */
    private BigDecimal volume;

    /** SKU图片 */
    private String image;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
