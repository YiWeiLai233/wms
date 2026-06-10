package com.yiweilai.wms.report.service.impl;

import com.yiweilai.wms.report.service.ReportService;
import com.yiweilai.wms.report.vo.DashboardVO;
import com.yiweilai.wms.report.vo.OutboundReportVO;
import com.yiweilai.wms.report.vo.StockReportVO;
import com.yiweilai.wms.stock.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表统计 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final StockMapper stockMapper;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();

        // 今日日期
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 今日订单数
        Long todayOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_order WHERE DATE(created_at) = ? AND deleted = 0",
                Long.class, today);
        vo.setTodayOrderCount(todayOrderCount != null ? todayOrderCount : 0L);

        // 待出库订单数
        Long pendingOutboundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_order WHERE order_status = 'WAIT_OUTBOUND' AND deleted = 0",
                Long.class);
        vo.setPendingOutboundCount(pendingOutboundCount != null ? pendingOutboundCount : 0L);

        // 今日出库单数
        Long todayOutboundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbound_order WHERE DATE(created_at) = ? AND deleted = 0",
                Long.class, today);
        vo.setTodayOutboundCount(todayOutboundCount != null ? todayOutboundCount : 0L);

        // 今日退货单数
        Long todayReturnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM return_order WHERE DATE(created_at) = ? AND deleted = 0",
                Long.class, today);
        vo.setTodayReturnCount(todayReturnCount != null ? todayReturnCount : 0L);

        // 库存预警数（可用数量 < 10）
        Long stockAlertCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock WHERE quantity < 10 AND deleted = 0",
                Long.class);
        vo.setStockAlertCount(stockAlertCount != null ? stockAlertCount : 0L);

        // 最近7天订单趋势
        List<DashboardVO.DayCount> orderTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sales_order WHERE DATE(created_at) = ? AND deleted = 0",
                    Long.class, dateStr);
            DashboardVO.DayCount dayCount = new DashboardVO.DayCount();
            dayCount.setDate(dateStr);
            dayCount.setCount(count != null ? count : 0L);
            orderTrend.add(dayCount);
        }
        vo.setOrderTrend(orderTrend);

        // 订单状态分布
        List<DashboardVO.StatusCount> statusDistribution = new ArrayList<>();
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("WAIT_PAY", "待付款");
        statusMap.put("WAIT_OUTBOUND", "待出库");
        statusMap.put("OUTBOUNDING", "出库中");
        statusMap.put("SHIPPED", "已发货");
        statusMap.put("FINISHED", "已完成");
        statusMap.put("CANCELLED", "已取消");

        for (Map.Entry<String, String> entry : statusMap.entrySet()) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sales_order WHERE order_status = ? AND deleted = 0",
                    Long.class, entry.getKey());
            DashboardVO.StatusCount statusCount = new DashboardVO.StatusCount();
            statusCount.setStatus(entry.getKey());
            statusCount.setStatusName(entry.getValue());
            statusCount.setCount(count != null ? count : 0L);
            statusDistribution.add(statusCount);
        }
        vo.setOrderStatusDistribution(statusDistribution);

        return vo;
    }

    @Override
    public StockReportVO getStockReport() {
        StockReportVO vo = new StockReportVO();

        // 总SKU数
        Long totalSkuCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT sku_id) FROM stock WHERE deleted = 0",
                Long.class);
        vo.setTotalSkuCount(totalSkuCount != null ? totalSkuCount : 0L);

        // 总库存数量
        Long totalQuantity = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE deleted = 0",
                Long.class);
        vo.setTotalQuantity(totalQuantity != null ? totalQuantity : 0L);

        // 总锁定数量
        Long totalLockedQty = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(locked_qty), 0) FROM stock WHERE deleted = 0",
                Long.class);
        vo.setTotalLockedQty(totalLockedQty != null ? totalLockedQty : 0L);

        // 总次品数量
        Long totalDefectiveQty = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(defective_qty), 0) FROM stock WHERE deleted = 0",
                Long.class);
        vo.setTotalDefectiveQty(totalDefectiveQty != null ? totalDefectiveQty : 0L);

        // 各仓库库存分布
        List<StockReportVO.WarehouseStock> warehouseStocks = jdbcTemplate.query(
                "SELECT s.warehouse_id, w.name as warehouse_name, " +
                "COALESCE(SUM(s.quantity), 0) as quantity, " +
                "COALESCE(SUM(s.locked_qty), 0) as locked_qty, " +
                "COALESCE(SUM(s.defective_qty), 0) as defective_qty " +
                "FROM stock s " +
                "LEFT JOIN warehouse w ON s.warehouse_id = w.id " +
                "WHERE s.deleted = 0 " +
                "GROUP BY s.warehouse_id, w.name",
                (rs, rowNum) -> {
                    StockReportVO.WarehouseStock ws = new StockReportVO.WarehouseStock();
                    ws.setWarehouseId(rs.getLong("warehouse_id"));
                    ws.setWarehouseName(rs.getString("warehouse_name"));
                    ws.setQuantity(rs.getLong("quantity"));
                    ws.setLockedQty(rs.getLong("locked_qty"));
                    ws.setDefectiveQty(rs.getLong("defective_qty"));
                    return ws;
                });
        vo.setWarehouseStocks(warehouseStocks);

        return vo;
    }

    @Override
    public OutboundReportVO getOutboundReport() {
        OutboundReportVO vo = new OutboundReportVO();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 今日出库单数
        Long todayOutboundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbound_order WHERE DATE(created_at) = ? AND deleted = 0",
                Long.class, today);
        vo.setTodayOutboundCount(todayOutboundCount != null ? todayOutboundCount : 0L);

        // 本月出库单数
        Long monthOutboundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbound_order WHERE DATE(created_at) >= ? AND deleted = 0",
                Long.class, monthStart);
        vo.setMonthOutboundCount(monthOutboundCount != null ? monthOutboundCount : 0L);

        // 待拣货单数
        Long pendingPickingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbound_order WHERE status = 'WAIT_PICKING' AND deleted = 0",
                Long.class);
        vo.setPendingPickingCount(pendingPickingCount != null ? pendingPickingCount : 0L);

        // 最近7天出库趋势
        List<DashboardVO.DayCount> outboundTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbound_order WHERE DATE(created_at) = ? AND deleted = 0",
                    Long.class, dateStr);
            DashboardVO.DayCount dayCount = new DashboardVO.DayCount();
            dayCount.setDate(dateStr);
            dayCount.setCount(count != null ? count : 0L);
            outboundTrend.add(dayCount);
        }
        vo.setOutboundTrend(outboundTrend);

        return vo;
    }
}
