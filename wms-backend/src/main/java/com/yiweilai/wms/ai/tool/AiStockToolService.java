package com.yiweilai.wms.ai.tool;

import com.yiweilai.wms.ai.dto.tool.AiStockQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.service.StockService;
import com.yiweilai.wms.stock.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiStockToolService {

    public static final String QUERY_SKU_INVENTORY = "query_sku_inventory";

    private final StockService stockService;
    private final AiToolLogService toolLogService;

    public AiToolResult<PageResult<StockVO>> querySkuInventory(AiStockQueryRequest request, AiToolContext context) {
        AiStockQueryRequest safeRequest = request == null ? new AiStockQueryRequest() : request;
        try {
            PageResult<StockVO> page = stockService.findByPage(toQuery(safeRequest));
            AiToolResult<PageResult<StockVO>> result = AiToolResult.success(
                    QUERY_SKU_INVENTORY, page, "Matched inventory records: " + nullSafeTotal(page));
            log(context, safeRequest, result, "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            AiToolResult<PageResult<StockVO>> result = AiToolResult.failed(QUERY_SKU_INVENTORY, message);
            log(context, safeRequest, result, "FAILED", message);
            return result;
        }
    }

    private StockQueryDTO toQuery(AiStockQueryRequest request) {
        StockQueryDTO query = new StockQueryDTO();
        query.setPage(page(request.getPage()));
        query.setSize(size(request.getSize()));
        query.setSkuId(request.getSkuId());
        query.setSkuCode(trimToNull(request.getSkuCode()));
        query.setSkuName(trimToNull(request.getSkuName()));
        query.setProductName(trimToNull(request.getProductName()));
        query.setWarehouseId(request.getWarehouseId());
        query.setStockType(trimToNull(request.getStockType()));
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
                QUERY_SKU_INVENTORY,
                request,
                response,
                status,
                errorMessage);
    }
}
