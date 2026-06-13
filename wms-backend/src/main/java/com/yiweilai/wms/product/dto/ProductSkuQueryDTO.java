package com.yiweilai.wms.product.dto;

import lombok.Data;

/**
 * SKU 查询参数
 */
@Data
public class ProductSkuQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 关键词（SKU名称/SKU编码） */
    private String keyword;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 仓库ID（筛选指定仓库库存） */
    private Long warehouseId;
}
