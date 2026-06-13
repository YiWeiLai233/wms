package com.yiweilai.wms.outbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 扫码核对参数
 */
@Data
public class OutboundScanDTO {

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    private Long outboundId;

    /** SKU编码或条码 */
    @NotBlank(message = "扫码内容不能为空")
    private String scanCode;

    /** 拣货货架ID */
    @NotNull(message = "拣货货架不能为空")
    private Long shelfId;
}
