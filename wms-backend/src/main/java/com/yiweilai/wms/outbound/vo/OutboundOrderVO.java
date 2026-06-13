package com.yiweilai.wms.outbound.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单返回对象
 */
@Data
public class OutboundOrderVO {

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

    /** 仓库名称 */
    private String warehouseName;

    /** 状态 */
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

    /** 出库明细列表 */
    private List<OutboundOrderItemVO> items;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
