package com.yiweilai.wms.outbound.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 出库明细实体
 */
@Data
public class OutboundOrderItem {

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

    /** 应出数量 */
    private Integer quantity;

    /** 已拣数量 */
    private Integer pickedQty;

    /** 拣货库位ID */
    private Long locationId;

    /** 是否已扫码确认：0-否 1-是 */
    private Integer scanned;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
