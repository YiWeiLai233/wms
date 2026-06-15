package com.yiweilai.wms.alert.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存预警模板实体
 */
@Data
public class StockAlertTemplate {

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String name;

    /** 低库存阈值 */
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    private Integer outOfStockThreshold;

    /** 是否启用：1-启用 0-禁用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0-未删 1-已删 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
