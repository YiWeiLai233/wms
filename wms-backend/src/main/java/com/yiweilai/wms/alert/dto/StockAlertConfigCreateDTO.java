package com.yiweilai.wms.alert.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增库存预警配置参数
 */
@Data
public class StockAlertConfigCreateDTO {

    /** SKU ID */
    @NotNull(message = "SKU不能为空")
    private Long skuId;

    /** 仓库ID，NULL表示所有仓库通用 */
    private Long warehouseId;

    /** 低库存阈值 */
    @NotNull(message = "低库存阈值不能为空")
    @Min(value = 0, message = "低库存阈值不能小于0")
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    @NotNull(message = "缺货阈值不能为空")
    @Min(value = 0, message = "缺货阈值不能小于0")
    private Integer outOfStockThreshold;

    /** 是否启用：1-启用 0-禁用 */
    private Integer enabled = 1;

    /** 备注 */
    private String remark;
}
