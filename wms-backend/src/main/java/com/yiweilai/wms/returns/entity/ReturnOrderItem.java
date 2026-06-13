package com.yiweilai.wms.returns.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 退货明细实体
 */
@Data
public class ReturnOrderItem {

    /** 明细ID */
    private Long id;

    /** 退货单ID */
    private Long returnId;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 码数 */
    private String sizeValue;

    /** 退货数量 */
    private Integer quantity;

    /** 质检状态：SELLABLE/DEFECTIVE/SCRAPPED */
    private String qualityStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
