package com.yiweilai.wms.stock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存调整参数
 */
@Data
public class StockAdjustDTO {

    /** SKU ID */
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 调整数量（正数增加，负数减少） */
    @NotNull(message = "调整数量不能为空")
    private Integer quantity;

    /** 备注 */
    private String remark;
}
