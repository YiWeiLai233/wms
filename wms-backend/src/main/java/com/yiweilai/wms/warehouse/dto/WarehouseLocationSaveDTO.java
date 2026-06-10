package com.yiweilai.wms.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库位新增/修改参数
 */
@Data
public class WarehouseLocationSaveDTO {

    /** 库位ID（修改时必填） */
    private Long id;

    /** 货架ID */
    @NotNull(message = "货架ID不能为空")
    private Long shelfId;

    /** 库位编码 */
    @NotBlank(message = "库位编码不能为空")
    private String code;

    /** 库位名称 */
    @NotBlank(message = "库位名称不能为空")
    private String name;

    /** 类型：1-普通库位 2-退货库位 3-次品库位 */
    private Integer type = 1;

    /** 容量上限 */
    private Integer capacity;

    /** 状态：1-启用 0-禁用 */
    private Integer status = 1;
}
