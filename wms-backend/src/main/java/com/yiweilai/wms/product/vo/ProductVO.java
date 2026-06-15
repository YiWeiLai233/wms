package com.yiweilai.wms.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情返回对象
 */
@Data
public class ProductVO {

    /** 商品ID */
    private Long id;

    /** SPU编码 */
    private String spuCode;

    /** 商品名称 */
    private String name;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 货架ID */
    private Long shelfId;

    /** 货架编码 */
    private String shelfCode;

    /** 货架名称 */
    private String shelfName;

    /** 分类名称 */
    private String categoryName;

    /** 主图URL */
    private String mainImage;

    /** 商品描述 */
    private String description;

    /** 参考售价 */
    private BigDecimal price;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 预警模板ID */
    private Long alertTemplateId;

    /** 预警模板名称 */
    private String alertTemplateName;

    /** SKU列表 */
    private List<ProductSkuVO> skuList;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
