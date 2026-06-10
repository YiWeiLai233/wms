package com.yiweilai.wms.product.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（SPU）
 */
@Data
public class Product {

    /** 商品ID */
    private Long id;

    /** SPU编码 */
    private String spuCode;

    /** 商品名称 */
    private String name;

    /** 货架ID */
    private Long shelfId;

    /** 主图URL */
    private String mainImage;

    /** 商品描述 */
    private String description;

    /** 参考售价 */
    private BigDecimal price;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
