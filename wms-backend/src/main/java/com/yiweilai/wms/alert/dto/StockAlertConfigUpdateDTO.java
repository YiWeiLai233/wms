package com.yiweilai.wms.alert.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改库存预警配置参数
 */
@Data
public class StockAlertConfigUpdateDTO {

    /** 低库存阈值 */
    @NotNull(message = "低库存阈值不能为空")
    @Min(value = 0, message = "低库存阈值不能小于0")
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    @NotNull(message = "缺货阈值不能为空")
    @Min(value = 0, message = "缺货阈值不能小于0")
    private Integer outOfStockThreshold;

    /** 是否启用：1-启用 0-禁用 */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
