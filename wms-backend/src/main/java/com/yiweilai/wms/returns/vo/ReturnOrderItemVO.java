package com.yiweilai.wms.returns.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退货明细返回对象
 */
@Data
public class ReturnOrderItemVO {

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

    /** 退货数量 */
    private Integer quantity;

    /** 质检状态 */
    private String qualityStatus;

    /** 入库库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
