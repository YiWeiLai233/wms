package com.yiweilai.wms.returns.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 退货单实体
 */
@Data
public class ReturnOrder {

    /** 退货单ID */
    private Long id;

    /** 退货单号 */
    private String returnNo;

    /** 原订单ID */
    private Long orderId;

    /** 原订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 仓库ID */
    private Long warehouseId;

    private String warehouseName;

    /** 状态：PENDING_CHECK/SELLABLE/DEFECTIVE/SCRAPPED */
    private String status;

    /** 退货原因 */
    private String reason;

    /** 客户退货快递单号 */
    private String trackingNo;

    /** 备注 */
    private String remark;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
