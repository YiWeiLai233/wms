package com.yiweilai.wms.stock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建盘点单参数
 */
@Data
public class StockCheckCreateDTO {

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 备注 */
    private String remark;
}
