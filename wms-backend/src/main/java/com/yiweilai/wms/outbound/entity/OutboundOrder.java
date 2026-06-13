package com.yiweilai.wms.outbound.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单实体
 */
@Data
public class OutboundOrder {

    /** 出库单ID */
    private Long id;

    /** 出库单号 */
    private String outboundNo;

    /** 关联订单ID */
    private Long orderId;

    /** 关联订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 仓库ID */
    private Long warehouseId;

    private String warehouseName;

    /** 状态：WAIT_PICKING/PICKING/SHIPPED */
    private String status;

    /** 拣货人ID */
    private Long pickerId;

    /** 拣货人姓名 */
    private String pickerName;

    /** 备注 */
    private String remark;

    /** 关联订单备注 */
    private String orderRemark;

    /** 快递单号 */
    private String trackingNo;

    /** 快递公司ID */
    private Long expressCompanyId;

    /** 快递公司名称 */
    private String expressCompanyName;

    /** 快递费用 */
    private BigDecimal shippingFee;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
