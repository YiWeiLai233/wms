package com.yiweilai.wms.alert.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存预警配置实体
 */
@Data
public class StockAlertConfig {

    /** 库存预警配置ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** 仓库ID，NULL表示所有仓库通用 */
    private Long warehouseId;

    /** 低库存阈值 */
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    private Integer outOfStockThreshold;

    /** 是否启用：1-启用 0-禁用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createdBy;

    /** 创建人名称 */
    private String createdByName;

    /** 更新人ID */
    private Long updatedBy;

    /** 更新人名称 */
    private String updatedByName;

    /** 逻辑删除：0-未删 1-已删 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
