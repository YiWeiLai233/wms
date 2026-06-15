package com.yiweilai.wms.order.dto;

import lombok.Data;

/**
 * 订单更新参数
 */
@Data
public class OrderUpdateDTO {

    /** 订单ID（从路径参数设置） */
    private Long id;

    /** 平台ID */
    private Long platformId;

    /** 平台订单号 */
    private String platformOrderNo;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件人地址 */
    private String receiverAddress;

    /** 备注 */
    private String remark;
}
