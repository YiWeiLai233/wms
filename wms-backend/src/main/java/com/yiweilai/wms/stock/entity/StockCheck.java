package com.yiweilai.wms.stock.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存盘点单实体
 */
@Data
public class StockCheck {

    /** 盘点单ID */
    private Long id;

    /** 盘点单号 */
    private String checkNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 状态：0-待盘点 1-盘点中 2-已完成 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
