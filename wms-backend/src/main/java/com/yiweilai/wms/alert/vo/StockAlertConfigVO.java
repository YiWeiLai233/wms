package com.yiweilai.wms.alert.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存预警配置返回对象
 */
@Data
public class StockAlertConfigVO {

    /** 库存预警配置ID */
    private Long id;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 低库存阈值 */
    private Integer lowStockThreshold;

    /** 缺货阈值 */
    private Integer outOfStockThreshold;

    /** 是否启用：1-启用 0-禁用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
