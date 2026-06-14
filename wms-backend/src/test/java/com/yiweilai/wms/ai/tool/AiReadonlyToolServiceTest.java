package com.yiweilai.wms.ai.tool;

import com.yiweilai.wms.ai.dto.tool.AiOrderQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiOutboundQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiReturnQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiStockQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.order.dto.OrderImportDTO;
import com.yiweilai.wms.order.dto.OrderQueryDTO;
import com.yiweilai.wms.order.dto.OrderStatusUpdateDTO;
import com.yiweilai.wms.order.service.OrderService;
import com.yiweilai.wms.order.vo.OrderVO;
import com.yiweilai.wms.outbound.dto.OutboundBatchCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.dto.OutboundQueryDTO;
import com.yiweilai.wms.outbound.dto.OutboundScanDTO;
import com.yiweilai.wms.outbound.service.OutboundService;
import com.yiweilai.wms.outbound.vo.OutboundOrderVO;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.dto.StockQueryDTO;
import com.yiweilai.wms.stock.service.StockService;
import com.yiweilai.wms.stock.vo.StockVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReadonlyToolServiceTest {

    private static final AiToolContext CONTEXT = AiToolContext.of(7L, 8L, 9L);

    @Test
    void queryOrderByNoUsesOnlyReadMethodsAndLogsCall() {
        OrderService orderService = mock(OrderService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        AiOrderToolService service = new AiOrderToolService(orderService, toolLogService);

        OrderVO match = new OrderVO();
        match.setId(11L);
        match.setOrderNo("SO202606140001");
        PageResult<OrderVO> page = new PageResult<>(1L, List.of(match), 1, 1);
        when(orderService.findByPage(any(OrderQueryDTO.class))).thenReturn(page);
        when(orderService.getById(11L)).thenReturn(match);

        AiOrderQueryRequest request = new AiOrderQueryRequest();
        request.setOrderNo("SO202606140001");

        AiToolResult<OrderVO> result = service.queryOrderByNo(request, CONTEXT);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getData()).isSameAs(match);
        verify(orderService).findByPage(any(OrderQueryDTO.class));
        verify(orderService).getById(11L);
        verify(orderService, never()).importOrder(any(OrderImportDTO.class));
        verify(orderService, never()).updateStatus(any(OrderStatusUpdateDTO.class));
        verify(toolLogService).recordToolCall(eq(7L), eq(8L), eq(9L),
                eq("query_order_by_no"), eq(request), eq(result), eq("SUCCESS"), isNull());
    }

    @Test
    void countPendingOutboundOrdersUsesWaitOutboundReadQuery() {
        OrderService orderService = mock(OrderService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        AiOrderToolService service = new AiOrderToolService(orderService, toolLogService);
        when(orderService.findByPage(any(OrderQueryDTO.class)))
                .thenReturn(new PageResult<>(12L, List.of(), 1, 1));

        AiToolResult<AiOrderToolService.PendingOutboundCount> result =
                service.countPendingOutboundOrders(CONTEXT);

        assertThat(result.getData().getCount()).isEqualTo(12L);
        verify(orderService).findByPage(org.mockito.ArgumentMatchers.argThat(query ->
                "WAIT_OUTBOUND".equals(query.getOrderStatus())
                        && Integer.valueOf(1).equals(query.getPage())
                        && Integer.valueOf(1).equals(query.getSize())));
        verify(orderService, never()).importOrder(any(OrderImportDTO.class));
        verify(orderService, never()).updateStatus(any(OrderStatusUpdateDTO.class));
    }

    @Test
    void querySkuInventoryUsesOnlyStockReadMethod() {
        StockService stockService = mock(StockService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        AiStockToolService service = new AiStockToolService(stockService, toolLogService);
        StockVO stock = new StockVO();
        stock.setSkuCode("SKU-1");
        when(stockService.findByPage(any(StockQueryDTO.class)))
                .thenReturn(new PageResult<>(1L, List.of(stock), 1, 10));

        AiStockQueryRequest request = new AiStockQueryRequest();
        request.setSkuCode("SKU-1");

        AiToolResult<PageResult<StockVO>> result = service.querySkuInventory(request, CONTEXT);

        assertThat(result.getData().getList()).containsExactly(stock);
        verify(stockService).findByPage(any(StockQueryDTO.class));
        verify(stockService, never()).adjust(any(StockAdjustDTO.class));
    }

    @Test
    void queryOutboundOrdersUsesOnlyOutboundReadMethod() {
        OutboundService outboundService = mock(OutboundService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        AiOutboundToolService service = new AiOutboundToolService(outboundService, toolLogService);
        when(outboundService.findByPage(any(OutboundQueryDTO.class)))
                .thenReturn(new PageResult<>(0L, List.of(), 1, 10));

        service.queryOutboundOrders(new AiOutboundQueryRequest(), CONTEXT);

        verify(outboundService).findByPage(any(OutboundQueryDTO.class));
        verify(outboundService, never()).getById(any());
        verify(outboundService, never()).create(any(OutboundCreateDTO.class));
        verify(outboundService, never()).createBatch(any(OutboundBatchCreateDTO.class));
        verify(outboundService, never()).scan(any(OutboundScanDTO.class));
        verify(outboundService, never()).confirm(any(OutboundConfirmDTO.class));
        verify(outboundService, never()).cancel(any());
    }

    @Test
    void queryReturnOrdersUsesOnlyReturnReadMethod() {
        ReturnService returnService = mock(ReturnService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        AiReturnToolService service = new AiReturnToolService(returnService, toolLogService);
        when(returnService.findByPage(any(ReturnQueryDTO.class)))
                .thenReturn(new PageResult<>(0L, List.of(), 1, 10));

        service.queryReturnOrders(new AiReturnQueryRequest(), CONTEXT);

        verify(returnService).findByPage(any(ReturnQueryDTO.class));
        verify(returnService, never()).getById(any());
        verify(returnService, never()).create(any(ReturnCreateDTO.class));
        verify(returnService, never()).check(any(ReturnCheckDTO.class));
        verify(returnService, never()).confirm(any());
        verify(returnService, never()).cancel(any());
        verify(returnService, never()).cancelByOrderId(any());
    }
}
