package com.yiweilai.wms.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单返回对象
 */
@Data
public class OrderVO {

    /** 订单ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 平台ID */
    private Long platformId;

    /** 平台名称 */
    private String platformName;

    /** 平台颜色 */
    private String platformColor;

    /** 快递公司 */
    private String expressCompany;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件人地址 */
    private String receiverAddress;

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

    /** 订单明细列表 */
    private List<OrderItemVO> items;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
