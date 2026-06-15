package com.yiweilai.wms.outbound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发货单更新参数
 */
@Data
public class OutboundUpdateDTO {

    /** 发货单ID */
    @NotNull(message = "发货单ID不能为空")
    private Long id;

    /** 快递单号 */
    private String trackingNo;

    /** 快递公司ID */
    private Long expressCompanyId;

    /** 快递费用 */
    private BigDecimal shippingFee;

    /** 备注 */
    private String remark;
}
