package com.yiweilai.wms.exchange.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 换货单返回对象
 */
@Data
public class ExchangeOrderVO {

    /** 换货单ID */
    private Long id;

    /** 换货单号 */
    private String exchangeNo;

    /** 关联订单ID */
    private Long orderId;

    /** 关联订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 状态 */
    private String status;

    /** 换货原因 */
    private String reason;

    /** 退回快递单号 */
    private String returnTrackingNo;

    /** 快递费用 */
    private BigDecimal shippingFee;

    /** 备注 */
    private String remark;

    /** 操作人ID */
    private Long operatorId;

    /** 换货明细列表 */
    private List<ExchangeOrderItemVO> items;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
