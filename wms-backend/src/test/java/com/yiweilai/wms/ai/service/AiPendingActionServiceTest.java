package com.yiweilai.wms.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.dto.action.AiActionExecuteResult;
import com.yiweilai.wms.ai.dto.action.AiCreateActionRequest;
import com.yiweilai.wms.ai.dto.action.AiPendingActionVO;
import com.yiweilai.wms.ai.entity.AiPendingAction;
import com.yiweilai.wms.ai.mapper.AiPendingActionMapper;
import com.yiweilai.wms.ai.service.impl.AiPendingActionServiceImpl;
import com.yiweilai.wms.ai.tool.AiWriteToolService;
import com.yiweilai.wms.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPendingActionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createPendingActionStoresPendingRecordWithExpiration() {
        AiPendingActionMapper mapper = mock(AiPendingActionMapper.class);
        AiWriteToolService writeToolService = mock(AiWriteToolService.class);
        AiPendingActionService service = new AiPendingActionServiceImpl(mapper, writeToolService, objectMapper);
        when(mapper.insert(any(AiPendingAction.class))).thenAnswer(invocation -> {
            AiPendingAction action = invocation.getArgument(0);
            action.setId(1001L);
            return 1;
        });

        AiCreateActionRequest request = new AiCreateActionRequest();
        request.setUserId(7L);
        request.setConversationId(8L);
        request.setActionType("create_outbound_order");
        request.setActionName("创建出库单");
        request.setRiskLevel("MEDIUM");
        request.setSummary("将为订单 11 创建出库单");
        request.setRequestParams(Map.of("orderId", 11L));

        AiPendingActionVO result = service.createPendingAction(request);

        assertThat(result.getActionId()).isEqualTo(1001L);
        assertThat(result.getNeedConfirm()).isTrue();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(mapper).insert(org.mockito.ArgumentMatchers.argThat(action ->
                Long.valueOf(7L).equals(action.getUserId())
                        && "create_outbound_order".equals(action.getActionType())
                        && "PENDING".equals(action.getStatus())
                        && action.getExpireAt().isAfter(LocalDateTime.now())
                        && action.getRequestParams().contains("\"orderId\":11")));
    }

    @Test
    void viewerCannotConfirmWriteActionAndBusinessToolIsNotCalled() {
        AiPendingActionMapper mapper = mock(AiPendingActionMapper.class);
        AiWriteToolService writeToolService = mock(AiWriteToolService.class);
        AiPendingActionService service = new AiPendingActionServiceImpl(mapper, writeToolService, objectMapper);
        AiPendingAction action = pendingAction("adjust_stock", "HIGH", "{\"skuId\":1,\"warehouseId\":1,\"quantity\":10}");
        when(mapper.findById(1001L)).thenReturn(action);

        assertThatThrownBy(() -> service.confirmAndExecute(1001L, 7L, "viewer", List.of("VIEWER")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");

        verify(writeToolService, never()).execute(any(), any(), any(), any());
        verify(mapper, never()).markExecuted(any(), any());
    }

    @Test
    void expiredActionCannotBeExecuted() {
        AiPendingActionMapper mapper = mock(AiPendingActionMapper.class);
        AiWriteToolService writeToolService = mock(AiWriteToolService.class);
        AiPendingActionService service = new AiPendingActionServiceImpl(mapper, writeToolService, objectMapper);
        AiPendingAction action = pendingAction("create_outbound_order", "MEDIUM", "{\"orderId\":11}");
        action.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(mapper.findById(1001L)).thenReturn(action);

        assertThatThrownBy(() -> service.confirmAndExecute(1001L, 7L, "admin", List.of("SUPER_ADMIN")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");

        verify(writeToolService, never()).execute(any(), any(), any(), any());
        verify(mapper).markExpired(1001L);
    }

    @Test
    void confirmExecutesWriteToolAndMarksActionExecuted() {
        AiPendingActionMapper mapper = mock(AiPendingActionMapper.class);
        AiWriteToolService writeToolService = mock(AiWriteToolService.class);
        AiPendingActionService service = new AiPendingActionServiceImpl(mapper, writeToolService, objectMapper);
        AiPendingAction action = pendingAction("create_outbound_order", "MEDIUM", "{\"orderId\":11}");
        when(mapper.findById(1001L)).thenReturn(action);
        AiActionExecuteResult executeResult = AiActionExecuteResult.success(1001L, "create_outbound_order", Map.of("outboundId", 22L));
        when(writeToolService.execute(eq(action), eq(7L), eq("admin"), eq(List.of("SUPER_ADMIN"))))
                .thenReturn(executeResult);

        AiActionExecuteResult result = service.confirmAndExecute(1001L, 7L, "admin", List.of("SUPER_ADMIN"));

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        verify(mapper).markConfirmed(1001L);
        verify(writeToolService).execute(action, 7L, "admin", List.of("SUPER_ADMIN"));
        verify(mapper).markExecuted(eq(1001L), org.mockito.ArgumentMatchers.contains("\"outboundId\":22"));
    }

    private AiPendingAction pendingAction(String actionType, String riskLevel, String requestParams) {
        AiPendingAction action = new AiPendingAction();
        action.setId(1001L);
        action.setUserId(7L);
        action.setConversationId(8L);
        action.setActionType(actionType);
        action.setActionName(actionType);
        action.setRequestParams(requestParams);
        action.setSummary("summary");
        action.setRiskLevel(riskLevel);
        action.setStatus("PENDING");
        action.setExpireAt(LocalDateTime.now().plusMinutes(20));
        return action;
    }
}
