package com.yiweilai.wms.outbound.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库明细返回对象
 */
@Data
public class OutboundOrderItemVO {

    /** 明细ID */
    private Long id;

    /** 出库单ID */
    private Long outboundId;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 码数 */
    private String sizeValue;

    /** 应出数量 */
    private Integer quantity;

    /** 已拣数量 */
    private Integer pickedQty;

    /** 拣货货架ID */
    private Long shelfId;

    /** 货架编码 */
    private String shelfCode;

    /** 是否已扫码确认 */
    private Integer scanned;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
