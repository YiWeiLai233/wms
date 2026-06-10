package com.yiweilai.wms.stock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交盘点结果参数
 */
@Data
public class StockCheckSubmitDTO {

    /** 盘点单ID */
    @NotNull(message = "盘点单ID不能为空")
    private Long checkId;

    /** 盘点明细列表 */
    @NotNull(message = "盘点明细不能为空")
    private List<StockCheckItemDTO> items;

    /**
     * 盘点明细项
     */
    @Data
    public static class StockCheckItemDTO {

        /** 明细ID */
        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        /** 实际盘点数量 */
        @NotNull(message = "实际数量不能为空")
        private Integer actualQty;
    }
}
