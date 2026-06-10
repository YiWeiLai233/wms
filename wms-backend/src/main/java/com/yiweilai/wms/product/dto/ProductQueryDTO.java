package com.yiweilai.wms.product.dto;

import lombok.Data;

/**
 * 商品查询参数
 */
@Data
public class ProductQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer size = 10;

    /** 关键词（名称/编码） */
    private String keyword;

    /** 货架ID */
    private Long shelfId;

    /** 状态 */
    private Integer status;
}
