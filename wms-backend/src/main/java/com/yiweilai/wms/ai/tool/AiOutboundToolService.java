package com.yiweilai.wms.ai.tool;

import com.yiweilai.wms.ai.dto.tool.AiOutboundQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.service.OutboundService;
import com.yiweilai.wms.outbound.vo.OutboundOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiOutboundToolService {

    public static final String QUERY_OUTBOUND_ORDERS = "query_outbound_orders";

    private final OutboundService outboundService;
    private final AiToolLogService toolLogService;

    public AiToolResult<Object> queryOutboundOrders(AiOutboundQueryRequest request, AiToolContext context) {
        AiOutboundQueryRequest safeRequest = request == null ? new AiOutboundQueryRequest() : request;
        try {
            Object data;
            String summary;
            if (safeRequest.getId() != null) {
                OutboundOrderVO detail = outboundService.getById(safeRequest.getId());
                data = detail;
                summary = "Outbound order found: " + detail.getOutboundNo();
            } else {
                PageResult<OutboundOrderVO> page = outboundService.findByPage(toQuery(safeRequest));
                data = page;
                summary = "Matched outbound orders: " + nullSafeTotal(page);
            }
            AiToolResult<Object> result = AiToolResult.success(QUERY_OUTBOUND_ORDERS, data, summary);
            log(context, safeRequest, result, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            AiToolResult<Object> result = AiToolResult.failed(QUERY_OUTBOUND_ORDERS, message);
            log(context, safeRequest, result, "FAILED", message);
            return result;
        }
    }

    private OutboundQueryDTO toQuery(AiOutboundQueryRequest request) {
        OutboundQueryDTO query = new OutboundQueryDTO();
        query.setPage(page(request.getPage()));
        query.setSize(size(request.getSize()));
        query.setOutboundNo(trimToNull(request.getOutboundNo()));
        query.setOrderNo(trimToNull(request.getOrderNo()));
        query.setPlatformOrderNo(trimToNull(request.getPlatformOrderNo()));
        query.setTrackingNo(trimToNull(request.getTrackingNo()));
        query.setStatus(trimToNull(request.getStatus()));
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

    private void log(AiToolContext context, Object request, Object response, String status, String errorMessage) {
        AiToolContext safeContext = context == null ? new AiToolContext() : context;
        toolLogService.recordToolCall(
                safeContext.getUserId(),
                safeContext.getConversationId(),
                safeContext.getMessageId(),
                QUERY_OUTBOUND_ORDERS,
                request,
                response,
                status,
                errorMessage);
    }
}
