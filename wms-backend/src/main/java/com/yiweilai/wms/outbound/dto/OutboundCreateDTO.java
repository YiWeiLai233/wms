package com.yiweilai.wms.outbound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建出库单参数
 */
@Data
public class OutboundCreateDTO {

    /** 订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 备注 */
    private String remark;
}
