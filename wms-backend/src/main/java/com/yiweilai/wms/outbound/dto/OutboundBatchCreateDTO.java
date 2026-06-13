package com.yiweilai.wms.outbound.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量创建出库单参数
 */
@Data
public class OutboundBatchCreateDTO {

    /** 订单ID列表 */
    @NotEmpty(message = "订单ID列表不能为空")
    private List<@NotNull(message = "订单ID不能为空") Long> orderIds;

    /** 备注 */
    private String remark;
}
