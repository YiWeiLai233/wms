package com.yiweilai.wms.returns.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建退货单参数
 */
@Data
public class ReturnCreateDTO {

    /** 原订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 退货原因 */
    @NotBlank(message = "退货原因不能为空")
    private String reason;

    /** 客户退货快递单号 */
    private String trackingNo;

    /** 退货快递费 */
    private BigDecimal shippingFee;

    /** 快递费用模板ID（用于自动计算快递费） */
    private Long feeTemplateId;

    /** 预估重量（用于自动计算快递费） */
    private BigDecimal estimatedWeight;

    /** 备注 */
    private String remark;

    /** 退货明细列表 */
    @Valid
    @NotEmpty(message = "Return items cannot be empty")
    private List<ReturnItemDTO> items;

    /**
     * 退货明细项
     */
    @Data
    public static class ReturnItemDTO {

        /** SKU ID */
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        /** 退货数量 */
        @NotNull(message = "退货数量不能为空")
        @Positive(message = "Return quantity must be greater than zero")
        private Integer quantity;
    }
}
