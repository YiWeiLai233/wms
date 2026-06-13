package com.yiweilai.wms.outbound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 确认出库参数
 */
@Data
public class OutboundConfirmDTO {

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    private Long outboundId;

    /** 快递单号 */
    private String trackingNo;

    /** 快递公司ID */
    private Long expressCompanyId;

    /** 费用模板ID */
    private Long feeTemplateId;

    /** 预估重量(kg) */
    private BigDecimal estimatedWeight;

    /** 快递费用（不传则根据模板自动计算） */
    private BigDecimal shippingFee;
}
