package com.yiweilai.wms.warehouse.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库区实体
 */
@Data
public class WarehouseArea {

    /** 库区ID */
    private Long id;

    /** 仓库ID */
    private Long warehouseId;

    /** 库区编码 */
    private String code;

    /** 库区名称 */
    private String name;

    /** 类型：1-普通区 2-退货区 3-次品区 */
    private Integer type;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
