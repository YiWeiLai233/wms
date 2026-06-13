package com.yiweilai.wms.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 退货质检参数
 */
@Data
public class ReturnCheckDTO {

    /** 退货单ID */
    @NotNull(message = "退货单ID不能为空")
    private Long returnId;

    /** 质检明细列表 */
    @NotNull(message = "质检明细不能为空")
    private List<ReturnCheckItemDTO> items;

    /**
     * 质检明细项
     */
    @Data
    public static class ReturnCheckItemDTO {

        /** 明细ID */
        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        /** 质检状态：SELLABLE/DEFECTIVE/SCRAPPED */
        @NotBlank(message = "质检状态不能为空")
        private String qualityStatus;
    }
}
