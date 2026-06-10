package com.yiweilai.wms.product.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类返回对象
 */
@Data
public class ProductCategoryVO {

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 父分类ID */
    private Long parentId;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 子分类列表 */
    private List<ProductCategoryVO> children;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
