package com.yiweilai.wms.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 库存报表
 */
@Data
public class StockReportVO {

    /** 总SKU数 */
    private Long totalSkuCount;

    /** 总库存数量 */
    private Long totalQuantity;

    /** 总锁定数量 */
    private Long totalLockedQty;

    /** 总次品数量 */
    private Long totalDefectiveQty;

    /** 各仓库库存分布 */
    private List<WarehouseStock> warehouseStocks;

    /**
     * 仓库库存
     */
    @Data
    public static class WarehouseStock {
        private Long warehouseId;
        private String warehouseName;
        private Long quantity;
        private Long lockedQty;
        private Long defectiveQty;
    }
}
