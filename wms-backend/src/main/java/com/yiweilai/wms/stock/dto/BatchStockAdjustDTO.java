package com.yiweilai.wms.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * 批量库存调整参数
 */
@Data
public class BatchStockAdjustDTO {

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 备注 */
    private String remark;

    /** 入库明细 */
    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<StockAdjustItem> items;

    @Data
    public static class StockAdjustItem {

        /** SKU ID */
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        /** 入库数量 */
        @NotNull(message = "入库数量不能为空")
        @Positive(message = "入库数量必须大于0")
        private Integer quantity;
    }
}
