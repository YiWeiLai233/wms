package com.yiweilai.wms.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 货架新增/修改参数
 */
@Data
public class WarehouseShelfSaveDTO {

    /** 货架ID（修改时必填） */
    private Long id;

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 货架编码 */
    @NotBlank(message = "货架编码不能为空")
    private String code;

    /** 货架名称 */
    @NotBlank(message = "货架名称不能为空")
    private String name;

    /** 商品分类 */
    private String categoryName;

    /** 状态：1-启用 0-禁用 */
    private Integer status = 1;
}
