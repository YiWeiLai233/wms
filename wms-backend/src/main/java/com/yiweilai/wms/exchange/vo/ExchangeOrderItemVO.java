package com.yiweilai.wms.exchange.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 换货明细返回对象
 */
@Data
public class ExchangeOrderItemVO {

    /** 明细ID */
    private Long id;

    /** 换货单ID */
    private Long exchangeId;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 码数 */
    private String sizeValue;

    /** 主图 */
    private String image;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 质量状态 */
    private String qualityStatus;

    /** 明细类型：RETURN_ITEM-退回商品 EXCHANGE_ITEM-换出商品 */
    private String itemType;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
