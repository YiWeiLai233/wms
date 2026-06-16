package com.yiweilai.wms.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页仪表盘数据
 */
@Data
public class DashboardVO {

    /** 今日订单数 */
    private Long todayOrderCount;

    /** 待出库订单数 */
    private Long pendingOutboundCount;

    /** 今日出库单数 */
    private Long todayOutboundCount;

    /** 今日退货单数 */
    private Long todayReturnCount;

    /** 库存预警数（库存低于阈值） */
    private Long stockAlertCount;

    /** 低库存数 */
    private Long lowStockCount;

    /** 缺货数 */
    private Long outOfStockCount;

    /** 最近7天订单趋势（按平台） */
    private List<PlatformTrend> platformTrends;

    /** 最近7天订单趋势（总计，兼容旧版） */
    private List<DayCount> orderTrend;

    /** 订单状态分布 */
    private List<StatusCount> orderStatusDistribution;

    /** 本月出货量TOP10 SKU */
    private List<SkuRank> topSkus;

    /** 近7天各平台SKU销量 */
    private List<PlatformSkuSales> platformSkuSales;

    /**
     * 平台趋势
     */
    @Data
    public static class PlatformTrend {
        private Long platformId;
        private String platformName;
        private String platformColor;
        private List<DayCount> data;
    }

    /**
     * 每日数量
     */
    @Data
    public static class DayCount {
        private String date;
        private Long count;
    }

    /**
     * 状态数量
     */
    @Data
    public static class StatusCount {
        private String status;
        private String statusName;
        private Long count;
    }

    /**
     * SKU出货排名
     */
    @Data
    public static class SkuRank {
        private Long skuId;
        private String skuCode;
        private String skuName;
        private Long totalQuantity;
        private List<PlatformQuantity> platformQuantities;
    }

    /**
     * 平台数量
     */
    @Data
    public static class PlatformQuantity {
        private Long platformId;
        private String platformName;
        private String platformColor;
        private Long quantity;
    }

    /**
     * 平台SKU销量
     */
    @Data
    public static class PlatformSkuSales {
        private Long platformId;
        private String platformName;
        private String platformColor;
        private List<SkuDaySales> skuSales;
    }

    /**
     * SKU每日销量
     */
    @Data
    public static class SkuDaySales {
        private String date;
        private String skuName;
        private Long quantity;
    }
}
