package com.yiweilai.wms.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类新增/修改参数
 */
@Data
public class ProductCategorySaveDTO {

    /** 分类ID（修改时必填） */
    private Long id;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 父分类ID */
    private Long parentId = 0L;

    /** 排序号 */
    private Integer sortOrder = 0;

    /** 状态：0-禁用 1-启用 */
    private Integer status = 1;
}
