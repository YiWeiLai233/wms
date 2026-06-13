package com.yiweilai.wms.returns.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退货单返回对象
 */
@Data
public class ReturnOrderVO {

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

    /** 仓库名称 */
    private String warehouseName;

    /** 状态 */
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

    /** 退货明细列表 */
    private List<ReturnOrderItemVO> items;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
