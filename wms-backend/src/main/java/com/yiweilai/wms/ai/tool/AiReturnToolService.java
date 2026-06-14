package com.yiweilai.wms.ai.tool;

import com.yiweilai.wms.ai.dto.tool.AiReturnQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiReturnToolService {

    public static final String QUERY_RETURN_ORDERS = "query_return_orders";

    private final ReturnService returnService;
    private final AiToolLogService toolLogService;

    public AiToolResult<Object> queryReturnOrders(AiReturnQueryRequest request, AiToolContext context) {
        AiReturnQueryRequest safeRequest = request == null ? new AiReturnQueryRequest() : request;
        try {
            Object data;
            String summary;
            if (safeRequest.getId() != null) {
                ReturnOrderVO detail = returnService.getById(safeRequest.getId());
                data = detail;
                summary = "Return order found: " + detail.getReturnNo();
            } else {
                PageResult<ReturnOrderVO> page = returnService.findByPage(toQuery(safeRequest));
                data = page;
                summary = "Matched return orders: " + nullSafeTotal(page);
            }
            AiToolResult<Object> result = AiToolResult.success(QUERY_RETURN_ORDERS, data, summary);
            log(context, safeRequest, result, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            AiToolResult<Object> result = AiToolResult.failed(QUERY_RETURN_ORDERS, message);
            log(context, safeRequest, result, "FAILED", message);
            return result;
        }
    }

    private ReturnQueryDTO toQuery(AiReturnQueryRequest request) {
        ReturnQueryDTO query = new ReturnQueryDTO();
        query.setPage(page(request.getPage()));
        query.setSize(size(request.getSize()));
        query.setReturnNo(trimToNull(request.getReturnNo()));
        query.setOrderNo(trimToNull(request.getOrderNo()));
        query.setPlatformOrderNo(trimToNull(request.getPlatformOrderNo()));
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
                QUERY_RETURN_ORDERS,
                request,
                response,
                status,
                errorMessage);
    }
}
