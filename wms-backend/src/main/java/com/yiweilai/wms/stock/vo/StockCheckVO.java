package com.yiweilai.wms.stock.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 盘点单返回对象
 */
@Data
public class StockCheckVO {

    /** 盘点单ID */
    private Long id;

    /** 盘点单号 */
    private String checkNo;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 状态：0-待盘点 1-盘点中 2-已完成 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 盘点明细列表 */
    private List<StockCheckItemVO> items;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
