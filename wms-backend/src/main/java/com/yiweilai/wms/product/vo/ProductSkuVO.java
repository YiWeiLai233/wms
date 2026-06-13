package com.yiweilai.wms.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SKU 返回对象
 */
@Data
public class ProductSkuVO {

    /** SKU ID */
    private Long id;

    /** 商品ID（SPU） */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String name;

    /** 码数，如 36、37、40 */
    private String sizeValue;

    /** 数量 */
    private Integer quantity;

    /** 可用库存 */
    private Integer availableQty;

    /** 锁定库存 */
    private Integer lockedQty;

    /** 次品库存 */
    private Integer defectiveQty;

    /** 总库存 */
    private Integer totalQty;

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

    /** 条码列表 */
    private List<ProductBarcodeVO> barcodeList;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
