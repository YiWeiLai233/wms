package com.yiweilai.wms.stock.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水返回对象
 */
@Data
public class StockLogVO {

    /** 流水ID */
    private Long id;

    /** 业务类型 */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** SKU ID */
    private Long skuId;

    /** SKU编码 */
    private String skuCode;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 库位ID */
    private Long locationId;

    /** 库位编码 */
    private String locationCode;

    /** 变动前数量 */
    private Integer quantityBefore;

    /** 变动数量 */
    private Integer quantityChange;

    /** 变动后数量 */
    private Integer quantityAfter;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
