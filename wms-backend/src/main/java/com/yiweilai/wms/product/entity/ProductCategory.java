package com.yiweilai.wms.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品分类实体
 */
@Data
public class ProductCategory {

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

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
