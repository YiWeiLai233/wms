package com.yiweilai.wms.exchange.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 换货单实体
 */
@Data
public class ExchangeOrder {

    /** 换货单ID */
    private Long id;

    /** 换货单号 */
    private String exchangeNo;

    /** 关联订单ID */
    private Long orderId;

    /** 关联订单号 */
    private String orderNo;

    /** 平台订单号（关联查询） */
    private String platformOrderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称（关联查询） */
    private String warehouseName;

    /** 状态：PENDING_RETURN/RETURNED/CHECKED/SHIPPED/COMPLETED/CANCELLED */
    private String status;

    /** 换货原因 */
    private String reason;

    /** 退回快递单号 */
    private String returnTrackingNo;

    /** 快递公司ID */
    private Long expressCompanyId;

    /** 快递公司名称（关联查询） */
    private String expressCompanyName;

    /** 快递费用 */
    private BigDecimal shippingFee;

    /** 备注 */
    private String remark;

    /** 操作人ID */
    private Long operatorId;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
