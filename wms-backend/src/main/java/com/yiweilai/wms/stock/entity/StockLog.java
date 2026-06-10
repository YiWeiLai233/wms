package com.yiweilai.wms.stock.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存流水实体
 */
@Data
public class StockLog {

    /** 流水ID */
    private Long id;

    /** 业务类型：INBOUND/OUTBOUND/RETURN/ADJUST/LOCK/RELEASE */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** SKU ID */
    private Long skuId;

    /** 仓库ID */
    private Long warehouseId;

    /** 库位ID */
    private Long locationId;

    /** 变动前数量 */
    private Integer quantityBefore;

    /** 变动数量（正数入库，负数出库） */
    private Integer quantityChange;

    /** 变动后数量 */
    private Integer quantityAfter;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
