package com.yiweilai.wms.report.service;

import com.yiweilai.wms.report.vo.DashboardVO;
import com.yiweilai.wms.report.vo.ExpressFeeReportVO;
import com.yiweilai.wms.report.vo.OutboundReportVO;
import com.yiweilai.wms.report.vo.StockReportVO;

import java.math.BigDecimal;

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

    /**
     * 快递费用统计
     */
    ExpressFeeReportVO getExpressFeeReport(String orderNo, String platformOrderNo, String startTime, String endTime, Long expressCompanyId);

    /**
     * 修改快递费用统计记录
     */
    void updateExpressFeeItem(String bizType, Long id, Long expressCompanyId, BigDecimal shippingFee);

    /**
     * 删除快递费用统计记录
     */
    void deleteExpressFeeItem(String bizType, Long id);
}
