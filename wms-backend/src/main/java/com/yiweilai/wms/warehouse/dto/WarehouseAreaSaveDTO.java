package com.yiweilai.wms.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库区新增/修改参数
 */
@Data
public class WarehouseAreaSaveDTO {

    /** 库区ID（修改时必填） */
    private Long id;

    /** 仓库ID */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /** 库区编码 */
    @NotBlank(message = "库区编码不能为空")
    private String code;

    /** 库区名称 */
    @NotBlank(message = "库区名称不能为空")
    private String name;

    /** 类型：1-普通区 2-退货区 3-次品区 */
    private Integer type = 1;

    /** 状态：1-启用 0-禁用 */
    private Integer status = 1;
}
