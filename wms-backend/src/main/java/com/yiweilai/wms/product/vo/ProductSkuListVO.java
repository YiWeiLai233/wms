package com.yiweilai.wms.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU 列表返回对象（含商品名和分类名）
 */
@Data
public class ProductSkuListVO {

    /** SKU ID */
    private Long id;

    /** 商品ID（SPU） */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String name;

    /** 码数，如 36、37、40 */
    private String sizeValue;

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

    /** Available inventory aggregated from stock. */
    private Integer availableQty;

    /** Locked inventory aggregated from stock. */
    private Integer lockedQty;

    /** Defective inventory aggregated from stock. */
    private Integer defectiveQty;

    /** Total inventory aggregated from stock. */
    private Integer totalQty;

    /** 商品名称 */
    private String productName;

    /** 货架号 */
    private String shelfCode;

    /** 分类名称 */
    private String categoryName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
