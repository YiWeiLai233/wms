package com.yiweilai.wms.report.vo;

import lombok.Data;

import java.util.List;

/**
 * 出库报表
 */
@Data
public class OutboundReportVO {

    /** 今日出库单数 */
    private Long todayOutboundCount;

    /** 本月出库单数 */
    private Long monthOutboundCount;

    /** 待拣货单数 */
    private Long pendingPickingCount;

    /** 最近7天出库趋势 */
    private List<DashboardVO.DayCount> outboundTrend;
}
