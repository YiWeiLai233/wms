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

        // 近7天各平台SKU销量
        List<DashboardVO.PlatformSkuSales> platformSkuSalesList = new ArrayList<>();
        List<Map<String, Object>> platformsForSku = jdbcTemplate.queryForList(
                "SELECT id, name, color FROM platform WHERE deleted = 0 AND enabled = 1 ORDER BY id");

        for (Map<String, Object> platform : platformsForSku) {
            Long pId = ((Number) platform.get("id")).longValue();
            String pName = (String) platform.get("name");
            String pColor = (String) platform.get("color");

            DashboardVO.PlatformSkuSales platformSales = new DashboardVO.PlatformSkuSales();
            platformSales.setPlatformId(pId);
            platformSales.setPlatformName(pName);
            platformSales.setPlatformColor(pColor != null ? pColor : "#94a3b8");

            List<DashboardVO.SkuDaySales> skuSalesList = jdbcTemplate.query(
                    "SELECT DATE(o.shipped_at) as sale_date, oi.sku_name, SUM(oi.quantity) as total_qty " +
                    "FROM outbound_order o " +
                    "JOIN outbound_order_item oi ON o.id = oi.outbound_id " +
                    "JOIN sales_order so ON o.order_id = so.id AND so.deleted = 0 " +
                    "WHERE o.status = 'SHIPPED' AND o.shipped_at >= ? AND so.platform_id = ? " +
                    "GROUP BY DATE(o.shipped_at), oi.sku_name " +
                    "ORDER BY sale_date, oi.sku_name",
                    (rs, rowNum) -> {
                        DashboardVO.SkuDaySales sds = new DashboardVO.SkuDaySales();
                        sds.setDate(rs.getString("sale_date"));
                        sds.setSkuName(rs.getString("sku_name"));
                        sds.setQuantity(rs.getLong("total_qty"));
                        return sds;
                    }, monthStart, pId);

            platformSales.setSkuSales(skuSalesList);
            platformSkuSalesList.add(platformSales);
        }

        vo.setPlatformSkuSales(platformSkuSalesList);

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
    public ExpressFeeReportVO getExpressFeeReport(String orderNo, String platformOrderNo, String startTime, String endTime, Long expressCompanyId) {
        ExpressFeeReportVO vo = new ExpressFeeReportVO();

        List<Object> outboundParams = new ArrayList<>();
        StringBuilder outboundWhere = new StringBuilder("WHERE oo.deleted = 0 AND oo.status = 'SHIPPED' AND oo.shipping_fee IS NOT NULL");

        List<Object> returnParams = new ArrayList<>();
        StringBuilder returnWhere = new StringBuilder("WHERE ro.deleted = 0 AND ro.status IN ('PENDING_CHECK','SELLABLE','DEFECTIVE','SCRAPPED','COMPLETED') AND ro.shipping_fee IS NOT NULL");

        if (orderNo != null && !orderNo.isEmpty()) {
            outboundWhere.append(" AND oo.order_no LIKE ?");
            outboundParams.add("%" + orderNo + "%");
            returnWhere.append(" AND ro.order_no LIKE ?");
            returnParams.add("%" + orderNo + "%");
        }
        if (platformOrderNo != null && !platformOrderNo.isEmpty()) {
            outboundWhere.append(" AND so.platform_order_no LIKE ?");
            outboundParams.add("%" + platformOrderNo + "%");
            returnWhere.append(" AND so.platform_order_no LIKE ?");
            returnParams.add("%" + platformOrderNo + "%");
        }
        if (startTime != null && !startTime.isEmpty()) {
            outboundWhere.append(" AND oo.shipped_at >= ?");
            outboundParams.add(startTime);
            returnWhere.append(" AND ro.created_at >= ?");
            returnParams.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            outboundWhere.append(" AND oo.shipped_at <= ?");
            outboundParams.add(endTime + " 23:59:59");
            returnWhere.append(" AND ro.created_at <= ?");
            returnParams.add(endTime + " 23:59:59");
        }
        if (expressCompanyId != null) {
            outboundWhere.append(" AND oo.express_company_id = ?");
            outboundParams.add(expressCompanyId);
            // 退货单没有快递公司字段，跳过此条件
        }

        // 查询出库汇总
        String outboundSummarySql = "SELECT COALESCE(SUM(oo.shipping_fee), 0) AS total_fee, COUNT(*) AS total_count " +
                "FROM outbound_order oo " +
                "LEFT JOIN sales_order so ON oo.order_id = so.id AND so.deleted = 0 " +
                outboundWhere;
        Map<String, Object> outboundSummary = jdbcTemplate.queryForMap(outboundSummarySql, outboundParams.toArray());
        BigDecimal outboundFee = outboundSummary.get("total_fee") != null ? new BigDecimal(outboundSummary.get("total_fee").toString()) : BigDecimal.ZERO;
        Long outboundCount = outboundSummary.get("total_count") != null ? ((Number) outboundSummary.get("total_count")).longValue() : 0L;
        vo.setOutboundFee(outboundFee);
        vo.setOutboundCount(outboundCount);

        // 查询退货汇总
        String returnSummarySql = "SELECT COALESCE(SUM(ro.shipping_fee), 0) AS total_fee, COUNT(*) AS total_count " +
                "FROM return_order ro " +
                "LEFT JOIN sales_order so ON ro.order_id = so.id AND so.deleted = 0 " +
                returnWhere;
        Map<String, Object> returnSummary = jdbcTemplate.queryForMap(returnSummarySql, returnParams.toArray());
        BigDecimal returnFee = returnSummary.get("total_fee") != null ? new BigDecimal(returnSummary.get("total_fee").toString()) : BigDecimal.ZERO;
        Long returnCount = returnSummary.get("total_count") != null ? ((Number) returnSummary.get("total_count")).longValue() : 0L;
        vo.setReturnFee(returnFee);
        vo.setReturnCount(returnCount);

        // 汇总
        vo.setTotalFee(outboundFee.add(returnFee));
        vo.setTotalCount(outboundCount + returnCount);

        // 查询出库明细
        String outboundDetailSql = "SELECT oo.id, oo.outbound_no AS biz_no, oo.order_no, so.platform_order_no, " +
                "'OUTBOUND' AS biz_type, '出库' AS biz_type_name, " +
                "oo.express_company_id, ec.name AS express_company_name, " +
                "oo.tracking_no, oo.shipping_fee, oo.shipped_at AS created_at " +
                "FROM outbound_order oo " +
                "LEFT JOIN sales_order so ON oo.order_id = so.id AND so.deleted = 0 " +
                "LEFT JOIN express_company ec ON oo.express_company_id = ec.id AND ec.deleted = 0 " +
                outboundWhere;

        List<ExpressFeeReportVO.ExpressFeeItem> outboundItems = jdbcTemplate.query(outboundDetailSql, (rs, rowNum) -> {
            ExpressFeeReportVO.ExpressFeeItem item = new ExpressFeeReportVO.ExpressFeeItem();
            item.setId(rs.getLong("id"));
            item.setBizNo(rs.getString("biz_no"));
            item.setOrderNo(rs.getString("order_no"));
            item.setPlatformOrderNo(rs.getString("platform_order_no"));
            item.setBizType(rs.getString("biz_type"));
            item.setBizTypeName(rs.getString("biz_type_name"));
            item.setExpressCompanyId(rs.getLong("express_company_id"));
            item.setExpressCompanyName(rs.getString("express_company_name"));
            item.setTrackingNo(rs.getString("tracking_no"));
            item.setShippingFee(rs.getBigDecimal("shipping_fee"));
            item.setCreatedAt(rs.getString("created_at"));
            return item;
        }, outboundParams.toArray());

        // 查询退货明细
        String returnDetailSql = "SELECT ro.id, ro.return_no AS biz_no, ro.order_no, so.platform_order_no, " +
                "'RETURN' AS biz_type, '退货' AS biz_type_name, " +
                "NULL AS express_company_id, '退货' AS express_company_name, " +
                "ro.tracking_no, ro.shipping_fee, ro.created_at " +
                "FROM return_order ro " +
                "LEFT JOIN sales_order so ON ro.order_id = so.id AND so.deleted = 0 " +
                returnWhere;

        List<ExpressFeeReportVO.ExpressFeeItem> returnItems = jdbcTemplate.query(returnDetailSql, (rs, rowNum) -> {
            ExpressFeeReportVO.ExpressFeeItem item = new ExpressFeeReportVO.ExpressFeeItem();
            item.setId(rs.getLong("id"));
            item.setBizNo(rs.getString("biz_no"));
            item.setOrderNo(rs.getString("order_no"));
            item.setPlatformOrderNo(rs.getString("platform_order_no"));
            item.setBizType(rs.getString("biz_type"));
            item.setBizTypeName(rs.getString("biz_type_name"));
            item.setExpressCompanyId(rs.getLong("express_company_id"));
            item.setExpressCompanyName(rs.getString("express_company_name"));
            item.setTrackingNo(rs.getString("tracking_no"));
            item.setShippingFee(rs.getBigDecimal("shipping_fee"));
            item.setCreatedAt(rs.getString("created_at"));
            return item;
        }, returnParams.toArray());

        // 合并并按时间排序
        List<ExpressFeeReportVO.ExpressFeeItem> allItems = new ArrayList<>();
        allItems.addAll(outboundItems);
        allItems.addAll(returnItems);
        allItems.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        vo.setItems(allItems);
        return vo;
    }
}
