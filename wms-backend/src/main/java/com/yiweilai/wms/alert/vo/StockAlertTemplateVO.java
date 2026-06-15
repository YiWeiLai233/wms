package com.yiweilai.wms.alert.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预警模板视图对象
 */
@Data
public class StockAlertTemplateVO {

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

    /** 关联商品数量 */
    private Integer productCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
