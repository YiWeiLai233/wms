package com.yiweilai.wms.warehouse.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库位实体
 */
@Data
public class WarehouseLocation {

    /** 库位ID */
    private Long id;

    /** 货架ID */
    private Long shelfId;

    /** 库位编码 */
    private String code;

    /** 库位名称 */
    private String name;

    /** 类型：1-普通库位 2-退货库位 3-次品库位 */
    private Integer type;

    /** 容量上限 */
    private Integer capacity;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
