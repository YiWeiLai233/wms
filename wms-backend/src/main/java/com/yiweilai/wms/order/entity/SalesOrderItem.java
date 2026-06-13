package com.yiweilai.wms.order.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体
 */
@Data
public class SalesOrderItem {

    /** 明细ID */
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 码数 */
    private String sizeValue;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 小计 */
    private BigDecimal totalPrice;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
