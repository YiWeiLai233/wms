package com.yiweilai.wms.report.service.impl;

import com.yiweilai.wms.report.service.ReportService;
import com.yiweilai.wms.report.vo.DashboardVO;
import com.yiweilai.wms.report.vo.ExpressFeeReportVO;
import com.yiweilai.wms.report.vo.OutboundReportVO;
import com.yiweilai.wms.report.vo.StockReportVO;
import com.yiweilai.wms.stock.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        // 最近7天订单趋势（按平台）
        List<DashboardVO.PlatformTrend> platformTrends = new ArrayList<>();
        List<Map<String, Object>> platforms = jdbcTemplate.queryForList(
                "SELECT id, name, color FROM platform WHERE deleted = 0 AND enabled = 1 ORDER BY id");

        // 获取最近7天的日期列表
        List<String> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        // 为每个平台统计数据
        for (Map<String, Object> platform : platforms) {
            Long platformId = ((Number) platform.get("id")).longValue();
            String platformName = (String) platform.get("name");
            String platformColor = (String) platform.get("color");

            DashboardVO.PlatformTrend trend = new DashboardVO.PlatformTrend();
            trend.setPlatformId(platformId);
            trend.setPlatformName(platformName);
            trend.setPlatformColor(platformColor != null ? platformColor : "#94a3b8");

            List<DashboardVO.DayCount> data = new ArrayList<>();
            for (String dateStr : dates) {
                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sales_order WHERE DATE(created_at) = ? AND platform_id = ? AND deleted = 0",
                        Long.class, dateStr, platformId);
                DashboardVO.DayCount dayCount = new DashboardVO.DayCount();
                dayCount.setDate(dateStr);
                dayCount.setCount(count != null ? count : 0L);
                data.add(dayCount);
            }
            trend.setData(data);
            platformTrends.add(trend);
        }

        // 统计未分配平台的订单
        DashboardVO.PlatformTrend noPlatformTrend = new DashboardVO.PlatformTrend();
        noPlatformTrend.setPlatformId(0L);
        noPlatformTrend.setPlatformName("其他");
        noPlatformTrend.setPlatformColor("#94a3b8");
        List<DashboardVO.DayCount> noPlatformData = new ArrayList<>();
        for (String dateStr : dates) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sales_order WHERE DATE(created_at) = ? AND platform_id IS NULL AND deleted = 0",
                    Long.class, dateStr);
            DashboardVO.DayCount dayCount = new DashboardVO.DayCount();
            dayCount.setDate(dateStr);
            dayCount.setCount(count != null ? count : 0L);
            noPlatformData.add(dayCount);
        }
        noPlatformTrend.setData(noPlatformData);
        platformTrends.add(noPlatformTrend);

        vo.setPlatformTrends(platformTrends);

        // 最近7天订单趋势（总计，兼容旧版）
        List<DashboardVO.DayCount> orderTrend = new ArrayList<>();
        for (String dateStr : dates) {
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

        // 本月出货量TOP10 SKU
        String monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<DashboardVO.SkuRank> topSkus = jdbcTemplate.query(
                "SELECT oi.sku_id, oi.sku_code, oi.sku_name, SUM(oi.quantity) AS total_quantity " +
                "FROM outbound_order_item oi " +
                "JOIN outbound_order o ON oi.outbound_id = o.id AND o.deleted = 0 " +
                "WHERE o.status = 'SHIPPED' AND o.shipped_at >= ? " +
                "GROUP BY oi.sku_id, oi.sku_code, oi.sku_name " +
                "ORDER BY total_quantity DESC " +
                "LIMIT 10",
                (rs, rowNum) -> {
                    DashboardVO.SkuRank rank = new DashboardVO.SkuRank();
                    rank.setSkuId(rs.getLong("sku_id"));
                    rank.setSkuCode(rs.getString("sku_code"));
                    rank.setSkuName(rs.getString("sku_name"));
                    rank.setTotalQuantity(rs.getLong("total_quantity"));
                    return rank;
                }, monthStart);

        // 查询各平台信息
        List<Map<String, Object>> platformList = jdbcTemplate.queryForList(
                "SELECT id, name, color FROM platform WHERE deleted = 0 AND enabled = 1 ORDER BY id");

        // 为每个SKU查询各平台的出货量
        for (DashboardVO.SkuRank rank : topSkus) {
            List<DashboardVO.PlatformQuantity> platformQuantities = new ArrayList<>();
            Long remainingQuantity = rank.getTotalQuantity();

            for (Map<String, Object> platform : platformList) {
                Long platformId = ((Number) platform.get("id")).longValue();
                String platformName = (String) platform.get("name");
                String platformColor = (String) platform.get("color");

                Long quantity = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(oi.quantity), 0) " +
                        "FROM outbound_order_item oi " +
                        "JOIN outbound_order o ON oi.outbound_id = o.id AND o.deleted = 0 " +
                        "JOIN sales_order so ON o.order_no = so.order_no AND so.deleted = 0 " +
                        "WHERE o.status = 'SHIPPED' AND o.shipped_at >= ? " +
                        "AND oi.sku_id = ? AND so.platform_id = ?",
                        Long.class, monthStart, rank.getSkuId(), platformId);

                quantity = quantity != null ? quantity : 0L;
                remainingQuantity -= quantity;

                DashboardVO.PlatformQuantity pq = new DashboardVO.PlatformQuantity();
                pq.setPlatformId(platformId);
                pq.setPlatformName(platformName);
                pq.setPlatformColor(platformColor != null ? platformColor : "#94a3b8");
                pq.setQuantity(quantity);
                platformQuantities.add(pq);
            }

            // 未分配平台的出货量
            if (remainingQuantity > 0) {
                DashboardVO.PlatformQuantity pq = new DashboardVO.PlatformQuantity();
                pq.setPlatformId(0L);
                pq.setPlatformName("其他");
                pq.setPlatformColor("#94a3b8");
                pq.setQuantity(remainingQuantity);
                platformQuantities.add(pq);
            }

            rank.setPlatformQuantities(platformQuantities);
        }

        vo.setTopSkus(topSkus);

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

        // 各仓库库存分布
        List<StockReportVO.WarehouseStock> warehouseStocks = jdbcTemplate.query(
                "SELECT s.warehouse_id, w.name as warehouse_name, " +
                "COALESCE(SUM(s.quantity), 0) as quantity, " +
                "COALESCE(SUM(s.locked_qty), 0) as locked_qty " +
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

    @Override
    public ExpressFeeReportVO getExpressFeeReport(String startTime, String endTime, Long expressCompanyId) {
        ExpressFeeReportVO vo = new ExpressFeeReportVO();

        // 构建查询条件
        StringBuilder where = new StringBuilder("WHERE oo.deleted = 0 AND oo.status = 'SHIPPED' AND oo.shipping_fee IS NOT NULL");
        List<Object> params = new ArrayList<>();

        if (startTime != null && !startTime.isEmpty()) {
            where.append(" AND oo.shipped_at >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            where.append(" AND oo.shipped_at <= ?");
            params.add(endTime + " 23:59:59");
        }
        if (expressCompanyId != null) {
            where.append(" AND oo.express_company_id = ?");
            params.add(expressCompanyId);
        }

        // 查询汇总
        String summarySql = "SELECT COALESCE(SUM(oo.shipping_fee), 0) AS total_fee, COUNT(*) AS total_count " +
                "FROM outbound_order oo " + where;
        Map<String, Object> summary = jdbcTemplate.queryForMap(summarySql, params.toArray());
        vo.setTotalFee(summary.get("total_fee") != null ? new BigDecimal(summary.get("total_fee").toString()) : BigDecimal.ZERO);
        vo.setTotalCount(summary.get("total_count") != null ? ((Number) summary.get("total_count")).longValue() : 0L);

        // 查询明细
        String detailSql = "SELECT oo.id AS outbound_id, oo.outbound_no, oo.order_no, " +
                "oo.express_company_id, ec.name AS express_company_name, " +
                "oo.tracking_no, oo.shipping_fee, oo.shipped_at " +
                "FROM outbound_order oo " +
                "LEFT JOIN express_company ec ON oo.express_company_id = ec.id AND ec.deleted = 0 " +
                where + " ORDER BY oo.shipped_at DESC";

        List<ExpressFeeReportVO.ExpressFeeItem> items = jdbcTemplate.query(detailSql, (rs, rowNum) -> {
            ExpressFeeReportVO.ExpressFeeItem item = new ExpressFeeReportVO.ExpressFeeItem();
            item.setOutboundId(rs.getLong("outbound_id"));
            item.setOutboundNo(rs.getString("outbound_no"));
            item.setOrderNo(rs.getString("order_no"));
            item.setExpressCompanyId(rs.getLong("express_company_id"));
            item.setExpressCompanyName(rs.getString("express_company_name"));
            item.setTrackingNo(rs.getString("tracking_no"));
            item.setShippingFee(rs.getBigDecimal("shipping_fee"));
            item.setShippedAt(rs.getString("shipped_at"));
            return item;
        }, params.toArray());

        vo.setItems(items);
        return vo;
    }
}
