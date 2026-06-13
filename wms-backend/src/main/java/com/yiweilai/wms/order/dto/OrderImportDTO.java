package com.yiweilai.wms.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单导入参数
 */
@Data
public class OrderImportDTO {

    /** 平台订单号 */
    private String platformOrderNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 收件人姓名 */
    @NotBlank(message = "收件人姓名不能为空")
    private String receiverName;

    /** 收件人电话 */
    @NotBlank(message = "收件人电话不能为空")
    private String receiverPhone;

    /** 收件人地址 */
    @NotBlank(message = "收件人地址不能为空")
    private String receiverAddress;

    /** 备注 */
    private String remark;

    /** 订单明细列表 */
    @NotEmpty(message = "订单明细不能为空")
    private List<OrderItemDTO> items;

    /**
     * 订单明细项
     */
    @Data
    public static class OrderItemDTO {

        /** SKU ID */
        private Long skuId;

        /** SKU编码 */
        @NotBlank(message = "SKU编码不能为空")
        private String skuCode;

        /** SKU名称 */
        private String skuName;

        /** 数量 */
        private Integer quantity;

        /** 单价 */
        private BigDecimal unitPrice;
    }
}
