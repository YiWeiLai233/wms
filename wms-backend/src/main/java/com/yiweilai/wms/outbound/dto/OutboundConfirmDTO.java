package com.yiweilai.wms.outbound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 确认出库参数
 */
@Data
public class OutboundConfirmDTO {

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    private Long outboundId;
}
