package com.yiweilai.wms.alert.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预警模板更新DTO
 */
@Data
public class StockAlertTemplateUpdateDTO {

    /** 模板ID（由路径参数设置） */
    private Long id;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** 低库存阈值 */
    @NotNull(message = "低库存阈值不能为空")
    @Min(value = 0, message = "低库存阈值不能小于0")
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    @NotNull(message = "缺货阈值不能为空")
    @Min(value = 0, message = "缺货阈值不能小于0")
    private Integer outOfStockThreshold;

    /** 是否启用 */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
