package com.yiweilai.wms.search.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单搜索返回对象
 */
@Data
public class OrderSearchVO {

    /** 订单ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件人地址 */
    private String receiverAddress;

    /** 订单状态 */
    private String orderStatus;

    /** 出库状态 */
    private String outboundStatus;

    /** 退货状态 */
    private String returnStatus;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** SKU编码列表 */
    private List<String> skuCodes;

    /** 商品名称列表 */
    private List<String> productNames;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
