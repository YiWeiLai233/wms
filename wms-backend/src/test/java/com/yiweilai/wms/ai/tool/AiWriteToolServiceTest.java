package com.yiweilai.wms.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.entity.AiPendingAction;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.log.entity.OperationLog;
import com.yiweilai.wms.log.service.OperationLogService;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.service.OutboundService;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.service.StockService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiWriteToolServiceTest {

    @Test
    void createOutboundCallsBusinessServiceAndWritesBothLogs() {
        OutboundService outboundService = mock(OutboundService.class);
        ReturnService returnService = mock(ReturnService.class);
        StockService stockService = mock(StockService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        AiWriteToolService service = new AiWriteToolService(
                outboundService, returnService, stockService, toolLogService, operationLogService, new ObjectMapper());
        when(outboundService.create(any(OutboundCreateDTO.class))).thenReturn(22L);

        AiPendingAction action = action("create_outbound_order", "{\"orderId\":11,\"remark\":\"AI\"}");

        var result = service.execute(action, 7L, "admin", List.of("SUPER_ADMIN"));

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        verify(outboundService).create(org.mockito.ArgumentMatchers.argThat(dto ->
                Long.valueOf(11L).equals(dto.getOrderId()) && "AI".equals(dto.getRemark())));
        verify(toolLogService).recordToolCall(eq(7L), eq(8L), eq(null),
                eq("create_outbound_order"), eq(action.getRequestParams()), any(), eq("SUCCESS"), eq(null));
        verify(operationLogService).saveLog(org.mockito.ArgumentMatchers.argThat(log ->
                Integer.valueOf(1).equals(log.getStatus())
                        && "AI_ASSISTANT:create_outbound_order".equals(log.getMethod())
                        && log.getParams().contains("\"orderId\":11")));
    }

    @Test
    void adjustStockAcceptsQuantityChangeAlias() {
        OutboundService outboundService = mock(OutboundService.class);
        ReturnService returnService = mock(ReturnService.class);
        StockService stockService = mock(StockService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        AiWriteToolService service = new AiWriteToolService(
                outboundService, returnService, stockService, toolLogService, operationLogService, new ObjectMapper());

        AiPendingAction action = action("adjust_stock", "{\"skuId\":1,\"warehouseId\":2,\"quantityChange\":10,\"remark\":\"AI\"}");

        service.execute(action, 7L, "admin", List.of("SUPER_ADMIN"));

        verify(stockService).adjust(org.mockito.ArgumentMatchers.argThat(dto ->
                Long.valueOf(1L).equals(dto.getSkuId())
                        && Long.valueOf(2L).equals(dto.getWarehouseId())
                        && Integer.valueOf(10).equals(dto.getQuantity())
                        && "AI".equals(dto.getRemark())));
    }

    private AiPendingAction action(String actionType, String requestParams) {
        AiPendingAction action = new AiPendingAction();
        action.setId(1001L);
        action.setUserId(7L);
        action.setConversationId(8L);
        action.setActionType(actionType);
        action.setActionName(actionType);
        action.setRequestParams(requestParams);
        action.setSummary("summary");
        action.setRiskLevel("HIGH");
        action.setStatus("PENDING");
        action.setExpireAt(LocalDateTime.now().plusMinutes(20));
        return action;
    }
}
