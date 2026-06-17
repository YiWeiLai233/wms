package com.yiweilai.wms.report.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.report.service.ReportService;
import com.yiweilai.wms.report.vo.DashboardVO;
import com.yiweilai.wms.report.vo.ExpressFeeReportVO;
import com.yiweilai.wms.report.vo.OutboundReportVO;
import com.yiweilai.wms.report.vo.StockReportVO;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yiweilai.wms.security.RequirePermission;

/**
 * 报表统计 Controller
 */
@RequirePermission("dashboard")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 获取首页仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(reportService.getDashboard());
    }

    /**
     * 获取库存报表
     */
    @GetMapping("/stock")
    public Result<StockReportVO> getStockReport() {
        return Result.success(reportService.getStockReport());
    }

    /**
     * 获取出库报表
     */
    @GetMapping("/outbound")
    public Result<OutboundReportVO> getOutboundReport() {
        return Result.success(reportService.getOutboundReport());
    }

    /**
     * 快递费用统计（按日期范围）
     */
    @GetMapping("/express-fee")
    public Result<ExpressFeeReportVO> getExpressFeeReport(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String platformOrderNo,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Long expressCompanyId) {
        return Result.success(reportService.getExpressFeeReport(orderNo, platformOrderNo, startTime, endTime, expressCompanyId));
    }
}
