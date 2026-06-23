package com.yiweilai.wms.exchange.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 换货单质检参数
 */
@Data
public class ExchangeCheckDTO {

    /** 换货单ID */
    @NotNull(message = "换货单ID不能为空")
    private Long exchangeId;

    /** 质检明细列表 */
    @Valid
    @NotEmpty(message = "质检明细不能为空")
    private List<ExchangeCheckItemDTO> items;

    /**
     * 质检明细项
     */
    @Data
    public static class ExchangeCheckItemDTO {

        /** 明细ID */
        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        /** 质量状态：SELLABLE/DEFECTIVE/SCRAPPED */
        @NotNull(message = "质量状态不能为空")
        private String qualityStatus;

        /** 退回仓库ID（可售商品入库目标仓库，不填则退回原发货仓） */
        private Long warehouseId;
    }
}
