package com.yiweilai.wms.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU 新增/修改参数
 */
@Data
public class ProductSkuSaveDTO {

    /** SKU ID（修改时必填） */
    private Long id;

    /** 商品ID（SPU） */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** SKU编码 */
    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    /** SKU名称 */
    @NotBlank(message = "SKU名称不能为空")
    private String name;

    /** 码数，如 36、37、40 */
    private String sizeValue;

    /** 数量 */
    private Integer quantity;

    /** Initial inbound quantity used when creating a SKU. */
    private Integer initialQuantity;

    /** Initial inbound warehouse ID. */
    private Long warehouseId;

    /** Initial inbound shelf ID. */
    private Long shelfId;

    /** Initial inbound remark. */
    private String inboundRemark;

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
    private Integer status = 1;
}
