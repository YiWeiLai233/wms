package com.yiweilai.wms.warehouse.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 货架实体
 */
@Data
public class WarehouseShelf {

    /** 货架ID */
    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 货架编码 */
    private String code;

    /** 货架名称 */
    private String name;

    /** 商品分类 */
    private String categoryName;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
