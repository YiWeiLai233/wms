package com.yiweilai.wms.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.ai.dto.action.AiActionExecuteResult;
import com.yiweilai.wms.ai.entity.AiPendingAction;
import com.yiweilai.wms.ai.service.AiToolLogService;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.log.entity.OperationLog;
import com.yiweilai.wms.log.service.OperationLogService;
import com.yiweilai.wms.outbound.dto.OutboundConfirmDTO;
import com.yiweilai.wms.outbound.dto.OutboundCreateDTO;
import com.yiweilai.wms.outbound.service.OutboundService;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.stock.dto.StockAdjustDTO;
import com.yiweilai.wms.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiWriteToolService {

    private final OutboundService outboundService;
    private final ReturnService returnService;
    private final StockService stockService;
    private final AiToolLogService toolLogService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public AiActionExecuteResult execute(AiPendingAction action, Long userId, String username, List<String> roles) {
        try {
            AiActionExecuteResult result = doExecute(action);
            recordLogs(action, userId, username, result.getResultData(), "SUCCESS", null);
            return result;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            recordLogs(action, userId, username, null, "FAILED", message);
            throw ex;
        }
    }

    private AiActionExecuteResult doExecute(AiPendingAction action) {
        String actionType = action.getActionType();
        return switch (actionType) {
            case "create_outbound_order" -> executeCreateOutbound(action);
            case "confirm_outbound_order" -> executeConfirmOutbound(action);
            case "create_return_order" -> executeCreateReturn(action);
            case "check_return_order" -> executeCheckReturn(action);
            case "confirm_return_order" -> executeConfirmReturn(action);
            case "adjust_stock" -> executeAdjustStock(action);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 AI 写操作");
        };
    }

    private AiActionExecuteResult executeCreateOutbound(AiPendingAction action) {
        OutboundCreateDTO dto = readParams(action, OutboundCreateDTO.class);
        if (dto.getOrderId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "orderId 不能为空");
        }
        Long outboundId = outboundService.create(dto);
        return AiActionExecuteResult.success(action.getId(), action.getActionType(), Map.of("outboundId", outboundId));
    }

    private AiActionExecuteResult executeConfirmOutbound(AiPendingAction action) {
        OutboundConfirmDTO dto = readParams(action, OutboundConfirmDTO.class);
        if (dto.getOutboundId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "outboundId 不能为空");
        }
        outboundService.confirm(dto);
        return AiActionExecuteResult.success(action.getId(), action.getActionType(), Map.of("outboundId", dto.getOutboundId()));
    }

    private AiActionExecuteResult executeCreateReturn(AiPendingAction action) {
        ReturnCreateDTO dto = readParams(action, ReturnCreateDTO.class);
        if (dto.getOrderId() == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "orderId 和退货明细不能为空");
        }
        Long returnId = returnService.create(dto);
        return AiActionExecuteResult.success(action.getId(), action.getActionType(), Map.of("returnId", returnId));
    }

    private AiActionExecuteResult executeCheckReturn(AiPendingAction action) {
        ReturnCheckDTO dto = readParams(action, ReturnCheckDTO.class);
        if (dto.getReturnId() == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "returnId 和质检明细不能为空");
        }
        returnService.check(dto);
        return AiActionExecuteResult.success(action.getId(), action.getActionType(), Map.of("returnId", dto.getReturnId()));
    }

    private AiActionExecuteResult executeConfirmReturn(AiPendingAction action) {
        Map<String, Object> params = readParamMap(action);
        Long returnId = toLong(params.get("returnId"));
        if (returnId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "returnId 不能为空");
        }
        returnService.confirm(returnId);
        return AiActionExecuteResult.success(action.getId(), action.getActionType(), Map.of("returnId", returnId));
    }

    private AiActionExecuteResult executeAdjustStock(AiPendingAction action) {
        Map<String, Object> params = readParamMap(action);
        if (!params.containsKey("quantity") && params.containsKey("quantityChange")) {
            params.put("quantity", params.get("quantityChange"));
        }
        params.remove("quantityChange");
        StockAdjustDTO dto = objectMapper.convertValue(params, StockAdjustDTO.class);
        if (dto.getSkuId() == null || dto.getWarehouseId() == null || dto.getQuantity() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "skuId、warehouseId 和 quantity 不能为空");
        }
        stockService.adjust(dto);
        return AiActionExecuteResult.success(action.getId(), action.getActionType(), Map.of(
                "skuId", dto.getSkuId(),
                "warehouseId", dto.getWarehouseId(),
                "quantity", dto.getQuantity()));
    }

    private <T> T readParams(AiPendingAction action, Class<T> type) {
        try {
            return objectMapper.readValue(action.getRequestParams(), type);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待执行参数解析失败");
        }
    }

    private Map<String, Object> readParamMap(AiPendingAction action) {
        try {
            return objectMapper.readValue(action.getRequestParams(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待执行参数解析失败");
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private void recordLogs(AiPendingAction action,
                            Long userId,
                            String username,
                            Object response,
                            String status,
                            String errorMessage) {
        toolLogService.recordToolCall(
                userId,
                action.getConversationId(),
                null,
                action.getActionType(),
                action.getRequestParams(),
                response,
                status,
                errorMessage);

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserName(username);
        log.setOperation(action.getActionName());
        log.setMethod("AI_ASSISTANT:" + action.getActionType());
        log.setParams(action.getRequestParams());
        log.setResult(toJson(response));
        log.setStatus("SUCCESS".equals(status) ? 1 : 0);
        log.setErrorMsg(errorMessage);
        operationLogService.saveLog(log);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode node) {
            return node.toString();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
