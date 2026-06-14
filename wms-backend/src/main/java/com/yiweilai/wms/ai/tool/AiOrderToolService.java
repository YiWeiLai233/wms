package com.yiweilai.wms.ai.tool;

import com.yiweilai.wms.ai.dto.tool.AiOrderQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiOrderSearchRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.order.dto.OrderQueryDTO;
import com.yiweilai.wms.order.service.OrderService;
import com.yiweilai.wms.order.vo.OrderVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class AiOrderToolService {

    public static final String QUERY_ORDER_BY_NO = "query_order_by_no";
    public static final String SEARCH_ORDERS = "search_orders";
    public static final String COUNT_PENDING_OUTBOUND_ORDERS = "count_pending_outbound_orders";

    private final OrderService orderService;
    private final AiToolLogService toolLogService;

    public AiToolResult<OrderVO> queryOrderByNo(AiOrderQueryRequest request, AiToolContext context) {
        AiOrderQueryRequest safeRequest = request == null ? new AiOrderQueryRequest() : request;
        try {
            if (!StringUtils.hasText(safeRequest.getOrderNo())
                    && !StringUtils.hasText(safeRequest.getPlatformOrderNo())) {
                return failed(QUERY_ORDER_BY_NO, context, safeRequest, "orderNo or platformOrderNo is required");
            }

            OrderVO order = findFirstOrder(safeRequest);
            String summary = order == null ? "No matching order found" : "Order found: " + order.getOrderNo();
            AiToolResult<OrderVO> result = AiToolResult.success(QUERY_ORDER_BY_NO, order, summary);
            log(context, QUERY_ORDER_BY_NO, safeRequest, result, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            return failed(QUERY_ORDER_BY_NO, context, safeRequest, ex);
        }
    }

    public AiToolResult<PageResult<OrderVO>> searchOrders(AiOrderSearchRequest request, AiToolContext context) {
        AiOrderSearchRequest safeRequest = request == null ? new AiOrderSearchRequest() : request;
        try {
            PageResult<OrderVO> page = shouldSearchKeywordAcrossFields(safeRequest)
                    ? searchKeywordAcrossFields(safeRequest)
                    : orderService.findByPage(toOrderQuery(safeRequest));
            AiToolResult<PageResult<OrderVO>> result = AiToolResult.success(
                    SEARCH_ORDERS, page, "Matched orders: " + nullSafeTotal(page));
            log(context, SEARCH_ORDERS, safeRequest, result, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            return failed(SEARCH_ORDERS, context, safeRequest, ex);
        }
    }

    public AiToolResult<PendingOutboundCount> countPendingOutboundOrders(AiToolContext context) {
        try {
            OrderQueryDTO query = new OrderQueryDTO();
            query.setPage(1);
            query.setSize(1);
            query.setOrderStatus("WAIT_OUTBOUND");
            PageResult<OrderVO> page = orderService.findByPage(query);
            PendingOutboundCount count = new PendingOutboundCount(nullSafeTotal(page));
            AiToolResult<PendingOutboundCount> result = AiToolResult.success(
                    COUNT_PENDING_OUTBOUND_ORDERS, count, "Pending outbound orders: " + count.getCount());
            log(context, COUNT_PENDING_OUTBOUND_ORDERS, null, result, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            return failed(COUNT_PENDING_OUTBOUND_ORDERS, context, null, ex);
        }
    }

    private OrderVO findFirstOrder(AiOrderQueryRequest request) {
        PageResult<OrderVO> page;
        if (StringUtils.hasText(request.getOrderNo())) {
            OrderQueryDTO query = new OrderQueryDTO();
            query.setPage(1);
            query.setSize(1);
            query.setOrderNo(request.getOrderNo().trim());
            page = orderService.findByPage(query);
            OrderVO order = firstDetailedOrder(page);
            if (order != null) {
                return order;
            }
        }
        if (StringUtils.hasText(request.getPlatformOrderNo())) {
            OrderQueryDTO query = new OrderQueryDTO();
            query.setPage(1);
            query.setSize(1);
            query.setPlatformOrderNo(request.getPlatformOrderNo().trim());
            page = orderService.findByPage(query);
            return firstDetailedOrder(page);
        }
        return null;
    }

    private OrderVO firstDetailedOrder(PageResult<OrderVO> page) {
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            return null;
        }
        OrderVO match = page.getList().get(0);
        return match.getId() == null ? match : orderService.getById(match.getId());
    }

    private boolean shouldSearchKeywordAcrossFields(AiOrderSearchRequest request) {
        return StringUtils.hasText(request.getKeyword())
                && !StringUtils.hasText(request.getOrderNo())
                && !StringUtils.hasText(request.getPlatformOrderNo())
                && !StringUtils.hasText(request.getReceiverName())
                && !StringUtils.hasText(request.getReceiverPhone());
    }

    private PageResult<OrderVO> searchKeywordAcrossFields(AiOrderSearchRequest request) {
        String keyword = request.getKeyword().trim();
        List<OrderVO> merged = new ArrayList<>();
        Set<Long> ids = new LinkedHashSet<>();
        List<BiConsumer<OrderQueryDTO, String>> fields = List.of(
                OrderQueryDTO::setOrderNo,
                OrderQueryDTO::setPlatformOrderNo,
                OrderQueryDTO::setReceiverName,
                OrderQueryDTO::setReceiverPhone);
        for (BiConsumer<OrderQueryDTO, String> field : fields) {
            OrderQueryDTO query = baseOrderQuery(request);
            field.accept(query, keyword);
            addUnique(merged, ids, orderService.findByPage(query));
        }
        return new PageResult<>((long) merged.size(), merged, page(request.getPage()), size(request.getSize()));
    }

    private void addUnique(List<OrderVO> merged, Set<Long> ids, PageResult<OrderVO> page) {
        if (page == null || page.getList() == null) {
            return;
        }
        for (OrderVO order : page.getList()) {
            if (order.getId() == null || ids.add(order.getId())) {
                merged.add(order);
            }
        }
    }

    private OrderQueryDTO toOrderQuery(AiOrderSearchRequest request) {
        OrderQueryDTO query = baseOrderQuery(request);
        query.setOrderNo(trimToNull(request.getOrderNo()));
        query.setPlatformOrderNo(trimToNull(request.getPlatformOrderNo()));
        query.setReceiverName(trimToNull(request.getReceiverName()));
        query.setReceiverPhone(trimToNull(request.getReceiverPhone()));
        if (StringUtils.hasText(request.getKeyword())) {
            query.setOrderNo(request.getKeyword().trim());
        }
        return query;
    }

    private OrderQueryDTO baseOrderQuery(AiOrderSearchRequest request) {
        OrderQueryDTO query = new OrderQueryDTO();
        query.setPage(page(request.getPage()));
        query.setSize(size(request.getSize()));
        query.setOrderStatus(trimToNull(request.getOrderStatus()));
        query.setWarehouseId(request.getWarehouseId());
        return query;
    }

    private int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int size(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private long nullSafeTotal(PageResult<?> page) {
        return page == null || page.getTotal() == null ? 0L : page.getTotal();
    }

    private String trimToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private <T> AiToolResult<T> failed(String toolName, AiToolContext context, Object request, RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return failed(toolName, context, request, message);
    }

    private <T> AiToolResult<T> failed(String toolName, AiToolContext context, Object request, String errorMessage) {
        AiToolResult<T> result = AiToolResult.failed(toolName, errorMessage);
        log(context, toolName, request, result, "FAILED", errorMessage);
        return result;
    }

    private void log(AiToolContext context, String toolName, Object request, Object response, String status, String errorMessage) {
        AiToolContext safeContext = context == null ? new AiToolContext() : context;
        toolLogService.recordToolCall(
                safeContext.getUserId(),
                safeContext.getConversationId(),
                safeContext.getMessageId(),
                toolName,
                request,
                response,
                status,
                errorMessage);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingOutboundCount {
        private Long count;
    }
}
