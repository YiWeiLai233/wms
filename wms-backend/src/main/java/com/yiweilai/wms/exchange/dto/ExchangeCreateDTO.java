package com.yiweilai.wms.exchange.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 换货单创建参数
 */
@Data
public class ExchangeCreateDTO {

    /** 关联订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 仓库ID */
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    /** 换货原因 */
    private String reason;

    /** 退回快递单号 */
    private String returnTrackingNo;

    /** 快递公司ID */
    private Long expressCompanyId;

    /** 快递费用 */
    private BigDecimal shippingFee;

    /** 备注 */
    private String remark;

    /** 换货明细列表 */
    @Valid
    @NotEmpty(message = "换货明细不能为空")
    private List<ExchangeItemDTO> items;

    /**
     * 换货明细项
     */
    @Data
    public static class ExchangeItemDTO {

        /** SKU ID */
        private Long skuId;

        /** SKU编码 */
        @NotBlank(message = "SKU编码不能为空")
        private String skuCode;

        /** SKU名称 */
        private String skuName;

        /** 码数 */
        private String sizeValue;

        /** 数量 */
        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须大于0")
        private Integer quantity;

        /** 单价 */
        private BigDecimal unitPrice;

        /** 明细类型：RETURN_ITEM-退回商品 EXCHANGE_ITEM-换出商品 */
        @NotBlank(message = "明细类型不能为空")
        private String itemType;
    }
}
