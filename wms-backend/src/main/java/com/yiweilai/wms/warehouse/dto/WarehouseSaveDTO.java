package com.yiweilai.wms.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 仓库新增/修改参数
 */
@Data
public class WarehouseSaveDTO {

    /** 仓库ID（修改时必填） */
    private Long id;

    /** 仓库编码 */
    @NotBlank(message = "仓库编码不能为空")
    private String code;

    /** 仓库名称 */
    @NotBlank(message = "仓库名称不能为空")
    private String name;

    /** 仓库地址 */
    private String address;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 状态：1-启用 0-禁用 */
    private Integer status = 1;
}
