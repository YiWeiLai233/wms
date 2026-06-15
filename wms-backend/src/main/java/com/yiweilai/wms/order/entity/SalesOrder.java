package com.yiweilai.wms.order.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
public class SalesOrder {

    /** 订单ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 快递公司 */
    private String expressCompany;

    /** 仓库ID */
    private Long warehouseId;

    private String warehouseName;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件人地址 */
    private String receiverAddress;

    /** Receiver name HMAC hash for exact lookup */
    private String receiverNameHash;

    /** Receiver phone HMAC hash for exact lookup */
    private String receiverPhoneHash;

    /** 订单状态 */
    private String orderStatus;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    /** 付款时间 */
    private LocalDateTime paidAt;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
