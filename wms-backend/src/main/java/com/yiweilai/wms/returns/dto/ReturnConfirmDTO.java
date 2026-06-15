package com.yiweilai.wms.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 退货确认入库参数
 */
@Data
public class ReturnConfirmDTO {

    /** 退货单ID */
    @NotNull(message = "退货单ID不能为空")
    private Long returnId;

    /** 质检明细列表（可选，用于更新质检结果） */
    private List<ReturnConfirmItemDTO> items;

    /**
     * 质检明细项
     */
    @Data
    public static class ReturnConfirmItemDTO {

        /** 明细ID */
        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        /** 质检状态：SELLABLE/DEFECTIVE/SCRAPPED */
        @NotBlank(message = "质检状态不能为空")
        private String qualityStatus;
    }
}
