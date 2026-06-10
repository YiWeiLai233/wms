package com.yiweilai.wms.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单状态更新参数
 */
@Data
public class OrderStatusUpdateDTO {

    /** 订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 目标状态 */
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;
}
