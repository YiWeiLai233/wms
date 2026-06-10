package com.yiweilai.wms.report.service;

import com.yiweilai.wms.report.vo.DashboardVO;
import com.yiweilai.wms.report.vo.OutboundReportVO;
import com.yiweilai.wms.report.vo.StockReportVO;

/**
 * 报表统计 Service
 */
public interface ReportService {

    /**
     * 获取首页仪表盘数据
     */
    DashboardVO getDashboard();

    /**
     * 获取库存报表
     */
    StockReportVO getStockReport();

    /**
     * 获取出库报表
     */
    OutboundReportVO getOutboundReport();
}
