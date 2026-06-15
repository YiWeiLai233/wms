package com.yiweilai.wms.ai.tool;

import com.yiweilai.wms.ai.dto.tool.AiOrderQueryRequest;
import com.yiweilai.wms.ai.dto.tool.AiToolContext;
import com.yiweilai.wms.ai.dto.tool.AiToolResult;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.order.dto.OrderQueryDTO;
import com.yiweilai.wms.order.service.OrderService;
import com.yiweilai.wms.order.vo.OrderVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiOrderToolPrivacyTest {

    @Test
    void queryOrderByNoReturnsMaskedReceiverFieldsToAiAndLogsMaskedData() {
        OrderService orderService = mock(OrderService.class);
        AiToolLogService toolLogService = mock(AiToolLogService.class);
        AiOrderToolService service = new AiOrderToolService(orderService, toolLogService);
        OrderVO order = order();
        when(orderService.findByPage(any(OrderQueryDTO.class)))
                .thenReturn(new PageResult<>(1L, List.of(order), 1, 1));
        when(orderService.getById(1L)).thenReturn(order);

        AiOrderQueryRequest request = new AiOrderQueryRequest();
        request.setOrderNo("SO-1");
        AiToolResult<OrderVO> result = service.queryOrderByNo(request, AiToolContext.of(1L, 2L, 3L));

        assertThat(result.getData().getReceiverName()).isEqualTo("A****");
        assertThat(result.getData().getReceiverPhone()).isEqualTo("138****8000");
        assertThat(result.getData().getReceiverAddress()).isEqualTo("Addr****");
        org.mockito.Mockito.verify(toolLogService).recordToolCall(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(AiOrderToolService.QUERY_ORDER_BY_NO),
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.argThat(response ->
                        response instanceof AiToolResult<?> toolResult
                                && toolResult.getData() instanceof OrderVO loggedOrder
                                && "138****8000".equals(loggedOrder.getReceiverPhone())),
                org.mockito.ArgumentMatchers.eq("SUCCESS"),
                org.mockito.ArgumentMatchers.isNull());
    }

    private OrderVO order() {
        OrderVO order = new OrderVO();
        order.setId(1L);
        order.setOrderNo("SO-1");
        order.setReceiverName("Alice");
        order.setReceiverPhone("13800138000");
        order.setReceiverAddress("Address 1");
        return order;
    }
}
